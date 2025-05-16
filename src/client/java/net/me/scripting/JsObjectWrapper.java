package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class JsObjectWrapper implements ProxyObject {
    private final Object javaInstance;
    private final Class<?> instanceClass;
    private final String instanceClassName;

    private final Map<String, List<String>> yarnToRuntimeMethodNames;
    private final Map<String, String> yarnToRuntimeFieldName;

    public JsObjectWrapper(Object instance,
                           Class<?> instanceClass,
                           Map<String, List<String>> methodLookup,
                           Map<String, String> fieldLookup
    ) {
        if (instance == null) {
            throw new NullPointerException("L'instance Java ne peut pas être nulle pour JsObjectWrapper.");
        }
        this.javaInstance = instance;
        this.instanceClass = (instanceClass != null) ? instanceClass : instance.getClass();
        this.instanceClassName = this.instanceClass.getName();

        this.yarnToRuntimeMethodNames = Map.copyOf(methodLookup);
        this.yarnToRuntimeFieldName = Map.copyOf(fieldLookup);
    }

    public Object getJavaInstance() {
        return this.javaInstance;
    }

    @Override
    public Object getMember(String yarnNameKey) {
        if (yarnToRuntimeMethodNames.containsKey(yarnNameKey)) {
            List<String> runtimeMethodNames = yarnToRuntimeMethodNames.get(yarnNameKey);

            List<Method> candidateInstanceMethods = ScriptUtils.findAndMakeAccessibleMethods(instanceClass, runtimeMethodNames, false);

            if ("execute".equals(yarnNameKey) && instanceClassName.contains("MinecraftClient")) {
                Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember for MinecraftClient.execute");
                Main.LOGGER.info("[MyQOLScript-Debug] Runtime names for 'execute': " + runtimeMethodNames);
                Main.LOGGER.info("[MyQOLScript-Debug] Candidate methods found: " + candidateInstanceMethods.size());
                if (!candidateInstanceMethods.isEmpty()) {
                    Main.LOGGER.info("[MyQOLScript-Debug] First candidate: " + candidateInstanceMethods.getFirst());
                }
            }

            return (ProxyExecutable) polyglotArgs -> {
                List<Method> matchingOverloads = new ArrayList<>();
                for (Method method : candidateInstanceMethods) {
                    if (method.getParameterCount() == polyglotArgs.length) {
                        matchingOverloads.add(method);
                    }
                }

                if (matchingOverloads.isEmpty()) {
                    String availableOverloads = candidateInstanceMethods.stream()
                            .map(m -> m.getParameterCount() + " args")
                            .distinct().collect(Collectors.joining(", "));
                    throw new RuntimeException(
                            String.format("Aucune surcharge de méthode d'instance nommée '%s' trouvée sur l'objet de classe %s avec %d arguments. Surcharges disponibles pour les méthodes nommées '%s' (toutes accessibilités): [%s]",
                                    yarnNameKey, instanceClassName, polyglotArgs.length, yarnNameKey, availableOverloads.isEmpty() ? "aucune avec ce nom" : availableOverloads));
                }

                Method methodToInvoke = matchingOverloads.getFirst();

                try {
                    Object[] javaArgs = ScriptUtils.unwrapArguments(polyglotArgs, methodToInvoke.getParameterTypes());
                    Object result = methodToInvoke.invoke(javaInstance, javaArgs);
                    return ScriptUtils.wrapReturnValue(result);
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    throw new RuntimeException(String.format("Erreur lors de l'invocation de la méthode d'instance %s.%s (runtime: %s) : %s",
                            instanceClassName, yarnNameKey, methodToInvoke.getName(), e.getMessage()), e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(String.format("La méthode d'instance %s.%s (runtime: %s) a levé une exception : %s",
                            instanceClassName, yarnNameKey, methodToInvoke.getName(), e.getCause().getMessage()), e.getCause());
                }
            };
        }

        if (yarnToRuntimeFieldName.containsKey(yarnNameKey)) {
            String runtimeFieldName = yarnToRuntimeFieldName.get(yarnNameKey);
            try {
                Field field = ScriptUtils.findAndMakeAccessibleField(instanceClass, runtimeFieldName);
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new RuntimeException(String.format(
                            "Tentative d'accès au champ statique '%s' (runtime: '%s') via un wrapper d'instance de %s. Utilisez le wrapper de classe.",
                            yarnNameKey, runtimeFieldName, instanceClassName));
                }
                return ScriptUtils.wrapReturnValue(field.get(javaInstance));
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(String.format("Champ d'instance '%s' (runtime: '%s') non trouvé sur l'objet de classe %s.",
                        yarnNameKey, runtimeFieldName, instanceClassName), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Impossible d'accéder au champ d'instance '%s' (runtime: '%s') sur l'objet de classe %s.",
                        yarnNameKey, runtimeFieldName, instanceClassName), e);
            }
        }

        return null;
    }

    @Override
    public boolean hasMember(String yarnNameKey) {
        return yarnToRuntimeMethodNames.containsKey(yarnNameKey) ||
                yarnToRuntimeFieldName.containsKey(yarnNameKey);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>();
        keys.addAll(yarnToRuntimeMethodNames.keySet());
        keys.addAll(yarnToRuntimeFieldName.keySet());
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String yarnNameKey, Value valueToSet) {
        if (yarnToRuntimeFieldName.containsKey(yarnNameKey)) {
            String runtimeFieldName = yarnToRuntimeFieldName.get(yarnNameKey);
            Field field = null;

            try {
                field = ScriptUtils.findAndMakeAccessibleField(instanceClass, runtimeFieldName);

                if (Modifier.isStatic(field.getModifiers())) {
                    throw new UnsupportedOperationException("The field should no be static.");
                }

                if (Modifier.isFinal(field.getModifiers())) {
                    throw new UnsupportedOperationException(String.format(
                            "Cannot modify FINAL instance field '%s' on object of class %s.",
                            yarnNameKey, instanceClassName));
                }

                Object javaValue = ScriptUtils.unwrapArguments(
                        new Value[]{valueToSet},
                        new Class<?>[]{field.getType()}
                )[0];

                field.set(javaInstance, javaValue);
                return;

            } catch (NoSuchFieldException e) {
                throw new RuntimeException(String.format(
                        "Champ d'instance '%s' (runtime: '%s') non trouvé pour l'assignation sur l'objet de classe %s.",
                        yarnNameKey, runtimeFieldName, instanceClassName), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format(
                        "Impossible d'assigner au champ d'instance '%s' (runtime: '%s') sur l'objet de classe %s en raison de restrictions d'accès.",
                        yarnNameKey, runtimeFieldName, instanceClassName), e);
            } catch (IllegalArgumentException e) {
                String fieldTypeName = (field != null) ? field.getType().getName() : "inconnu (champ non résolu)";
                throw new RuntimeException(String.format(
                        "Type de valeur incompatible pour l'assignation au champ d'instance '%s' (runtime: '%s', type attendu: %s) sur l'objet de classe %s.",
                        yarnNameKey, runtimeFieldName, fieldTypeName, instanceClassName), e);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("Tentative d'accès au champ statique")) {
                    throw e;
                }
                throw new RuntimeException(String.format(
                        "Erreur lors de la tentative d'assignation au champ d'instance '%s' (runtime: '%s') sur l'objet de classe %s: %s",
                        yarnNameKey, runtimeFieldName, instanceClassName, e.getMessage()), e);
            }
        }

        throw new UnsupportedOperationException(
                String.format("Le membre '%s' n'est pas un champ modifiable connu (ou non trouvé) sur les objets de classe %s.",
                        yarnNameKey, instanceClassName));
    }

}