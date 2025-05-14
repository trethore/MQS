package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
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
                            Object[] javaArgs = ScriptManager.unwrapArguments(polyglotArgs, ctor.getParameterTypes());
                            Object instance = ctor.newInstance(javaArgs);
                            return ScriptManager.wrapReturnValue(instance);
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

            List<Method> candidateStaticMethods = new ArrayList<>();
            for (Method m : targetClass.getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && runtimeNamesForYarnKey.contains(m.getName())) {
                    candidateStaticMethods.add(m);
                }
            }

            return (ProxyExecutable) polyglotArgs -> {
                for (Method method : candidateStaticMethods) {
                    if (method.getParameterCount() == polyglotArgs.length) {
                        try {
                            Object[] javaArgs = ScriptManager.unwrapArguments(polyglotArgs, method.getParameterTypes());
                            Object result = method.invoke(null, javaArgs); // 'null' pour l'instance cible des méthodes statiques
                            return ScriptManager.wrapReturnValue(result);
                        } catch (IllegalAccessException | IllegalArgumentException e) {
                            throw new RuntimeException(String.format("Erreur lors de l'invocation de la méthode statique %s.%s (runtime: %s) : %s",
                                    targetClassName, yarnNameKey, method.getName(), e.getMessage()), e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(String.format("La méthode statique %s.%s (runtime: %s) a levé une exception : %s",
                                    targetClassName, yarnNameKey, method.getName(), e.getCause().getMessage()), e.getCause());
                        }
                    }
                }
                String availableOverloads = candidateStaticMethods.stream()
                        .map(m -> m.getParameterCount() + " args")
                        .distinct()
                        .collect(Collectors.joining(", "));
                throw new RuntimeException(
                        String.format("Aucune surcharge de méthode statique nommée '%s' trouvée sur la classe %s avec %d arguments. Surcharges disponibles pour '%s': [%s]",
                                yarnNameKey, targetClassName, polyglotArgs.length, yarnNameKey, availableOverloads.isEmpty() ? "aucune avec ce nom" : availableOverloads));
            };
        }

        if (yarnToRuntimeFieldName.containsKey(yarnNameKey)) {
            String runtimeFieldName = yarnToRuntimeFieldName.get(yarnNameKey);
            try {
                Field field = targetClass.getField(runtimeFieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    throw new RuntimeException(String.format("Le champ '%s' (runtime: '%s') sur la classe %s n'est pas statique.",
                            yarnNameKey, runtimeFieldName, targetClassName));
                }
                return ScriptManager.wrapReturnValue(field.get(null));
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(String.format("Champ statique '%s' (runtime: '%s') non trouvé sur la classe %s.",
                        yarnNameKey, runtimeFieldName, targetClassName), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Impossible d'accéder au champ statique '%s' (runtime: '%s') sur la classe %s.",
                        yarnNameKey, runtimeFieldName, targetClassName), e);
            }
        }

        return null;
    }

    @Override
    public boolean hasMember(String yarnNameKey) {
        return "new".equals(yarnNameKey) ||
                yarnToRuntimeMethodNames.containsKey(yarnNameKey) ||
                yarnToRuntimeFieldName.containsKey(yarnNameKey);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>();
        keys.add("new");
        keys.addAll(yarnToRuntimeMethodNames.keySet());
        keys.addAll(yarnToRuntimeFieldName.keySet());
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException(
                String.format("La modification des membres statiques de la classe %s n'est pas supportée.", targetClassName));
    }
}
