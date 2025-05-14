package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
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

            List<Method> candidateInstanceMethods = new ArrayList<>();
            for (Method m : instanceClass.getMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) && runtimeMethodNames.contains(m.getName())) {
                    candidateInstanceMethods.add(m);
                }
            }

            return (ProxyExecutable) polyglotArgs -> {
                for (Method method : candidateInstanceMethods) {
                    if (method.getParameterCount() == polyglotArgs.length) {
                        try {
                            Object[] javaArgs = ScriptManager.unwrapArguments(polyglotArgs, method.getParameterTypes());
                            Object result = method.invoke(javaInstance, javaArgs);
                            return ScriptManager.wrapReturnValue(result);
                        } catch (IllegalAccessException | IllegalArgumentException e) {
                            throw new RuntimeException(String.format("Erreur lors de l'invocation de la méthode d'instance %s.%s (runtime: %s) : %s",
                                    instanceClassName, yarnNameKey, method.getName(), e.getMessage()), e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(String.format("La méthode d'instance %s.%s (runtime: %s) a levé une exception : %s",
                                    instanceClassName, yarnNameKey, method.getName(), e.getCause().getMessage()), e.getCause());
                        }
                    }
                }
                String availableOverloads = candidateInstanceMethods.stream()
                        .map(m -> m.getParameterCount() + " args")
                        .distinct()
                        .collect(Collectors.joining(", "));
                throw new RuntimeException(
                        String.format("Aucune surcharge de méthode d'instance nommée '%s' trouvée sur l'objet de classe %s avec %d arguments. Surcharges disponibles pour '%s': [%s]",
                                yarnNameKey, instanceClassName, polyglotArgs.length, yarnNameKey, availableOverloads.isEmpty() ? "aucune avec ce nom" : availableOverloads));
            };
        }

        if (yarnToRuntimeFieldName.containsKey(yarnNameKey)) {
            String runtimeFieldName = yarnToRuntimeFieldName.get(yarnNameKey);
            try {
                Field field = getField(yarnNameKey, runtimeFieldName);
                return ScriptManager.wrapReturnValue(field.get(javaInstance));
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

    private @NotNull Field getField(String yarnNameKey, String runtimeFieldName) throws NoSuchFieldException {
        Field field = instanceClass.getField(runtimeFieldName);
        if (Modifier.isStatic(field.getModifiers())) {
            throw new RuntimeException(String.format("Tentative d'accès au champ statique '%s' (runtime: '%s') via un wrapper d'instance de %s. Utilisez le wrapper de classe.",
                    yarnNameKey, runtimeFieldName, instanceClassName));
        }
        return field;
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
                field = getAccessibleField(yarnNameKey, runtimeFieldName);

                if (Modifier.isFinal(field.getModifiers())) {
                    throw new UnsupportedOperationException(String.format(
                            "Impossible de modifier le champ final '%s' (runtime: '%s') sur l'objet de classe %s.",
                            yarnNameKey, runtimeFieldName, instanceClassName));
                }

                Object javaValue = ScriptManager.unwrapArguments(
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
    private @NotNull Field getAccessibleField(String yarnNameKey, String runtimeFieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field;
        try {
            field = instanceClass.getField(runtimeFieldName);
        } catch (NoSuchFieldException e) {
            Main.LOGGER.error("Impossible de trouver le champ public '{}' (runtime: '{}') sur l'objet de classe {}. Vérifiez la correspondance des noms.",
                    yarnNameKey, runtimeFieldName, instanceClassName);
            throw e;
        }

        if (Modifier.isStatic(field.getModifiers())) {
            throw new RuntimeException(
                    String.format("Tentative d'accès au champ statique '%s' (runtime: '%s') via un wrapper d'instance de %s. Utilisez le wrapper de classe.",
                            yarnNameKey, runtimeFieldName, instanceClassName));
        }

        return field;
    }
}