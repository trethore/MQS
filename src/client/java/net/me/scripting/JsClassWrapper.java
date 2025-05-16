package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsClassWrapper implements ProxyObject {
    private final Class<?> targetClass;
    private final String targetClassName;
    private final Map<String, List<String>> yarnToRuntimeMethodNames;
    private final Map<String, String> yarnToRuntimeFieldName;

    public JsClassWrapper(String runtimeFqcn,
                          Map<String, List<String>> methodLookup,
                          Map<String, String> fieldLookup
    ) throws ClassNotFoundException {
        Main.LOGGER.debug("Création de JsClassWrapper pour : {}", runtimeFqcn);
        this.targetClass = Class.forName(runtimeFqcn);
        this.targetClassName = this.targetClass.getName();
        this.yarnToRuntimeMethodNames = Map.copyOf(methodLookup);
        this.yarnToRuntimeFieldName = Map.copyOf(fieldLookup);
    }

    @Override
    public Object getMember(String yarnNameKey) {
        if ("new".equals(yarnNameKey)) {
            return (ProxyExecutable) polyglotArgs -> {
                Constructor<?>[] constructors = targetClass.getConstructors();
                for (Constructor<?> ctor : constructors) {
                    if (ctor.getParameterCount() == polyglotArgs.length) {
                        try {
                            Object[] javaArgs = ScriptUtils.unwrapArguments(polyglotArgs, ctor.getParameterTypes());
                            Object instance = ctor.newInstance(javaArgs);
                            return ScriptUtils.wrapReturnValue(instance);
                        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                            throw new RuntimeException(String.format("Erreur lors de l'instanciation de %s avec %d arguments : %s",
                                    targetClassName, polyglotArgs.length, e.getMessage()), e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(String.format("Le constructeur de %s a levé une exception : %s",
                                    targetClassName, e.getCause().getMessage()), e.getCause());
                        }
                    }
                }
                String availableConstructors = Stream.of(constructors)
                        .map(c -> c.getParameterCount() + " args")
                        .distinct()
                        .collect(Collectors.joining(", "));
                throw new RuntimeException(
                        String.format("Aucun constructeur trouvé pour la classe %s avec %d arguments. Constructeurs disponibles : [%s]",
                                targetClassName, polyglotArgs.length, availableConstructors.isEmpty() ? "aucun public" : availableConstructors));
            };
        }

        if (yarnToRuntimeMethodNames.containsKey(yarnNameKey)) {
            List<String> runtimeNamesForYarnKey = yarnToRuntimeMethodNames.get(yarnNameKey);

            List<Method> candidateStaticMethods = ScriptUtils.findAndMakeAccessibleMethods(targetClass, runtimeNamesForYarnKey, true /*isStatic*/);

            return (ProxyExecutable) polyglotArgs -> {
                List<Method> matchingOverloads = new ArrayList<>();
                for (Method method : candidateStaticMethods) {
                    if (method.getParameterCount() == polyglotArgs.length) {
                        matchingOverloads.add(method);
                    }
                }

                if (matchingOverloads.isEmpty()) {
                    String availableOverloads = candidateStaticMethods.stream()
                            .map(m -> m.getParameterCount() + " args")
                            .distinct().collect(Collectors.joining(", "));
                    throw new RuntimeException(
                            String.format("Aucune surcharge de méthode statique nommée '%s' trouvée sur la classe %s avec %d arguments. Surcharges disponibles pour les méthodes nommées '%s' (toutes accessibilités): [%s]",
                                    yarnNameKey, targetClassName, polyglotArgs.length, yarnNameKey, availableOverloads.isEmpty() ? "aucune avec ce nom" : availableOverloads));
                }

                Method methodToInvoke = matchingOverloads.getFirst();

                try {
                    Object[] javaArgs = ScriptUtils.unwrapArguments(polyglotArgs, methodToInvoke.getParameterTypes());
                    Object result = methodToInvoke.invoke(null, javaArgs);
                    return ScriptUtils.wrapReturnValue(result);
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    throw new RuntimeException(String.format("Erreur lors de l'invocation de la méthode statique %s.%s (runtime: %s) : %s",
                            targetClassName, yarnNameKey, methodToInvoke.getName(), e.getMessage()), e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(String.format("La méthode statique %s.%s (runtime: %s) a levé une exception : %s",
                            targetClassName, yarnNameKey, methodToInvoke.getName(), e.getCause().getMessage()), e.getCause());
                }
            };
        }

        if (yarnToRuntimeFieldName.containsKey(yarnNameKey)) {
            String runtimeFieldName = yarnToRuntimeFieldName.get(yarnNameKey);
            try {
                Field field = ScriptUtils.findAndMakeAccessibleField(targetClass, runtimeFieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    throw new RuntimeException(String.format("Le champ '%s' (runtime: '%s') sur la classe %s n'est pas statique.",
                            yarnNameKey, runtimeFieldName, targetClassName));
                }
                return ScriptUtils.wrapReturnValue(field.get(null));
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(String.format("Champ statique '%s' (runtime: '%s') non trouvé sur la classe %s.",
                        yarnNameKey, runtimeFieldName, targetClassName), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Impossible d'accéder au champ statique '%s' (runtime: '%s') sur la classe %s.",
                        yarnNameKey, runtimeFieldName, targetClassName), e);
            }
        }
        if ("_class".equals(yarnNameKey)) {
            return this.targetClass;
        }

        return null;
    }

    @Override
    public boolean hasMember(String yarnNameKey) {
        return "new".equals(yarnNameKey) || "_class".equals(yarnNameKey) ||
                yarnToRuntimeMethodNames.containsKey(yarnNameKey) ||
                yarnToRuntimeFieldName.containsKey(yarnNameKey);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>();
        keys.add("new");
        keys.add("_class");
        keys.addAll(yarnToRuntimeMethodNames.keySet());
        keys.addAll(yarnToRuntimeFieldName.keySet());
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String key, Value value) {
        if (yarnToRuntimeFieldName.containsKey(key)) {
            String runtimeFieldName = yarnToRuntimeFieldName.get(key);
            Field field = null;

            try {
                field = ScriptUtils.findAndMakeAccessibleField(targetClass, runtimeFieldName);

                if (!Modifier.isStatic(field.getModifiers())) {
                    throw new UnsupportedOperationException(String.format(
                            "Le champ '%s' (runtime: '%s') sur la classe %s n'est pas statique. Modification via instance requise.",
                            key, runtimeFieldName, targetClassName));
                }

                if (Modifier.isFinal(field.getModifiers())) {
                    throw new UnsupportedOperationException(String.format(
                            "Cannot modify FINAL static field '%s' on class %s.",
                            key, targetClassName));
                }


                Object javaValue = ScriptUtils.unwrapArguments(
                        new Value[]{value},
                        new Class<?>[]{field.getType()}
                )[0];

                field.set(null, javaValue);
                return;

            } catch (NoSuchFieldException e) {
                throw new RuntimeException(String.format(
                        "Champ statique '%s' (runtime: '%s') non trouvé pour l'assignation sur la classe %s.",
                        key, runtimeFieldName, targetClassName), e);
            } catch (IllegalAccessException e) {

                throw new RuntimeException(String.format(
                        "Impossible d'assigner au champ statique '%s' (runtime: '%s') sur la classe %s en raison de restrictions d'accès.",
                        key, runtimeFieldName, targetClassName), e);
            } catch (IllegalArgumentException e) {
                String fieldTypeName = (field != null) ? field.getType().getName() : "inconnu (champ non résolu)";
                throw new RuntimeException(String.format(
                        "Type de valeur incompatible pour l'assignation au champ statique '%s' (runtime: '%s', type attendu: %s) sur la classe %s.",
                        key, runtimeFieldName, fieldTypeName, targetClassName), e);
            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "Erreur inattendue lors de la tentative d'assignation au champ statique '%s' (runtime: '%s') sur la classe %s: %s",
                        key, runtimeFieldName, targetClassName, e.getMessage()), e);
            }
        }

        throw new UnsupportedOperationException(
                String.format("Le membre '%s' n'est pas un champ statique modifiable connu (ou non trouvé) sur la classe %s, ou la modification de ce type de membre n'est pas supportée.",
                        key, targetClassName));
    }
}
