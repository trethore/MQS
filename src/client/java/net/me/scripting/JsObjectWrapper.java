package net.me.scripting;

import net.me.Main;
import net.me.mappings.MappingsManager;
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
    private final String instanceClassName; // Runtime FQCN
    private final String yarnInstanceClassName; // Yarn FQCN

    private final Map<String, List<String>> yarnToRuntimeMethodNames;
    private final Map<String, String> yarnToRuntimeFieldName;

    private static final String MINECRAFT_CLIENT_YARN_FQCN = "net.minecraft.client.MinecraftClient";

    public JsObjectWrapper(Object instance,
                           Class<?> instanceClass, // Peut être null, sera dérivé de 'instance'
                           Map<String, List<String>> methodLookup,
                           Map<String, String> fieldLookup
    ) {
        if (instance == null) {
            throw new NullPointerException("L'instance Java ne peut pas être nulle pour JsObjectWrapper.");
        }
        this.javaInstance = instance;
        this.instanceClass = (instanceClass != null) ? instanceClass : instance.getClass();
        this.instanceClassName = this.instanceClass.getName();

        Map<String, String> runtimeToYarnMap = MappingsManager.getInstance().getRuntimeToYarnClassMap();
        if (runtimeToYarnMap == null) {
            Main.LOGGER.warn("JsObjectWrapper Constructor: runtimeToYarnClassMap is null while constructing wrapper for {}", this.instanceClassName);
            this.yarnInstanceClassName = "";
        } else {
            String yarnName = runtimeToYarnMap.get(this.instanceClassName);
            this.yarnInstanceClassName = (yarnName != null) ? yarnName : "";
            if (this.yarnInstanceClassName.isEmpty() && !this.instanceClassName.startsWith("java.")) {
                Main.LOGGER.warn("JsObjectWrapper Constructor: No YARN FQCN found for non-java runtime class: {}", this.instanceClassName);
            }
        }
        Main.LOGGER.debug("JsObjectWrapper created for runtime class: {}, yarn class: {} (Lookup map size: {})",
                this.instanceClassName,
                this.yarnInstanceClassName.isEmpty() ? "(not a mapped class or issue)" : this.yarnInstanceClassName,
                runtimeToYarnMap != null ? runtimeToYarnMap.size() : "null map");


        this.yarnToRuntimeMethodNames = Map.copyOf(methodLookup != null ? methodLookup : Collections.emptyMap());
        this.yarnToRuntimeFieldName = Map.copyOf(fieldLookup != null ? fieldLookup : Collections.emptyMap());

        // Log spécifique pour MinecraftClient et la présence de 'execute'
        if (MINECRAFT_CLIENT_YARN_FQCN.equals(this.yarnInstanceClassName)) {
            Main.LOGGER.info("JsObjectWrapper for MinecraftClient (Yarn: {}): methods mapped: {}, fields mapped: {}. Method 'execute' present in yarnToRuntimeMethodNames: {}",
                    this.yarnInstanceClassName, this.yarnToRuntimeMethodNames.size(), this.yarnToRuntimeFieldName.size(), this.yarnToRuntimeMethodNames.containsKey("execute"));
        } else {
            Main.LOGGER.debug("JsObjectWrapper for {} (Yarn: {}): methods mapped: {}, fields mapped: {}.",
                    this.instanceClassName, this.yarnInstanceClassName.isEmpty() ? "N/A" : this.yarnInstanceClassName,
                    this.yarnToRuntimeMethodNames.size(), this.yarnToRuntimeFieldName.size());
        }
    }

    public Object getJavaInstance() {
        return this.javaInstance;
    }

    @Override
    public Object getMember(String yarnNameKey) {
        Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember ENTER for key: '{}' on instance of {} ({})", yarnNameKey, this.yarnInstanceClassName, this.instanceClassName);

        if ("_IS_MY_QOL_PROXY_".equals(yarnNameKey)) {
            Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember IS_PROXY check for key: '{}'", yarnNameKey);
            return true;
        }

        String effectiveYarnNameKey;
        boolean isMcExecuteTestAlias;

        boolean isMinecraftClientInstance = MINECRAFT_CLIENT_YARN_FQCN.equals(this.yarnInstanceClassName);
        Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember: Values before alias logic -> yarnNameKey='{}', isMinecraftClientInstance={}, yarnInstanceClassName='{}'",
                yarnNameKey, isMinecraftClientInstance, this.yarnInstanceClassName);


        if ("doTheExecution".equals(yarnNameKey) && isMinecraftClientInstance) {
            Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember: 'doTheExecution' alias detected for MCClient. Setting effectiveYarnNameKey to 'execute'.");
            effectiveYarnNameKey = "execute";
            isMcExecuteTestAlias = true;
        } else {
            effectiveYarnNameKey = yarnNameKey;
            isMcExecuteTestAlias = false;
            if ("doTheExecution".equals(yarnNameKey) && !isMinecraftClientInstance) {
                Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember: 'doTheExecution' key seen, but instance is NOT MinecraftClient (yarn: {}). effectiveYarnNameKey remains '{}'.", this.yarnInstanceClassName, effectiveYarnNameKey);
            }
        }
        Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember: Values after alias logic -> effectiveYarnNameKey='{}', isMcExecuteTestAlias={}",
                effectiveYarnNameKey, isMcExecuteTestAlias);

        boolean isAttemptingMcExecute = "execute".equals(effectiveYarnNameKey) && isMinecraftClientInstance;
        Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember: Calculated isAttemptingMcExecute={}", isAttemptingMcExecute);

        // 1. Priorité aux méthodes mappées par Yarn
        if (yarnToRuntimeMethodNames.containsKey(effectiveYarnNameKey)) {
            List<String> runtimeMethodNames = yarnToRuntimeMethodNames.get(effectiveYarnNameKey);
            List<Method> candidateInstanceMethods = ScriptUtils.findAndMakeAccessibleMethods(instanceClass, runtimeMethodNames, false);

            if (isAttemptingMcExecute) { // Log plus détaillé pour MC.execute
                Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember: MAPPED Yarn method '{}' (called via '{}'). Runtime names: {}. Candidates found by ScriptUtils: {}", effectiveYarnNameKey, yarnNameKey, runtimeMethodNames, candidateInstanceMethods.size());
                if (!candidateInstanceMethods.isEmpty()) {
                    Method firstCandidate = candidateInstanceMethods.getFirst();
                    Main.LOGGER.info("[MyQOLScript-Debug] First candidate (mapped): {} (Declaring class: {})", firstCandidate, firstCandidate.getDeclaringClass().getName());
                } else {
                    Main.LOGGER.warn("[MyQOLScript-Debug] No MAPPED candidate instance methods found for runtime names: {}.", runtimeMethodNames);
                }
            }

            if (!candidateInstanceMethods.isEmpty()) {
                final String outerYarnNameKey = yarnNameKey;
                final String outerEffectiveYarnNameKey = effectiveYarnNameKey;
                // Copier runtimeMethodNames pour le lambda si nécessaire, ou le récupérer à l'intérieur
                final List<String> finalRuntimeMethodNames = runtimeMethodNames;


                return (ProxyExecutable) polyglotArgs -> {
                    if (isAttemptingMcExecute) {
                        Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper: ProxyExecutable for MAPPED {}.{} (called via '{}') INVOKED.", this.yarnInstanceClassName, outerEffectiveYarnNameKey, outerYarnNameKey);
                        // ... (logs des arguments)
                    }

                    List<Method> matchingOverloads = new ArrayList<>();
                    for (Method method : candidateInstanceMethods) {
                        if (method.getParameterCount() == polyglotArgs.length) {
                            matchingOverloads.add(method);
                        }
                    }

                    if (matchingOverloads.isEmpty()) {
                        String availableOverloads = candidateInstanceMethods.stream()
                                .map(m -> m.getName() + "(" + Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ")) + ") - " + m.getParameterCount() + " args")
                                .distinct().collect(Collectors.joining("; "));
                        throw new RuntimeException(
                                String.format("MAPPED: Aucune surcharge de méthode d'instance nommée '%s' (effective: '%s', runtime names: %s) trouvée sur l'objet de classe %s (Yarn: %s) avec %d arguments. Surcharges candidates pour les méthodes nommées '%s' (toutes accessibilités): [%s]",
                                        outerYarnNameKey, outerEffectiveYarnNameKey, finalRuntimeMethodNames, instanceClassName, yarnInstanceClassName, polyglotArgs.length, outerEffectiveYarnNameKey, availableOverloads.isEmpty() ? "aucune avec ce nom" : availableOverloads));
                    }

                    Method methodToInvoke = matchingOverloads.getFirst();
                    if (isAttemptingMcExecute) {
                        Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper MAPPED ProxyExecutable: Method to invoke: {}", methodToInvoke);
                    }
                    try {
                        Object[] javaArgs = ScriptUtils.unwrapArguments(polyglotArgs, methodToInvoke.getParameterTypes());
                        Object result = methodToInvoke.invoke(javaInstance, javaArgs);
                        return ScriptUtils.wrapReturnValue(result);
                    } catch (Exception e) { // Catch plus large pour logging
                        Main.LOGGER.error("[MyQOLScript-Debug] Error invoking MAPPED method {}.{}: {}", instanceClassName, outerEffectiveYarnNameKey, e.getMessage(), e);
                        throw new RuntimeException(String.format("Erreur lors de l'invocation de la méthode d'instance MAPPÉE %s.%s (runtime: %s) : %s",
                                instanceClassName, outerYarnNameKey, methodToInvoke.getName(), e.getMessage()), e);
                    }
                };
            }
        }

        // 2. Si non trouvée dans les méthodes mappées par Yarn, essayer la réflexion Java directe.
        //    On utilise yarnNameKey ici car c'est ce que le script JS a appelé.
        if (isAttemptingMcExecute || !yarnToRuntimeMethodNames.containsKey(effectiveYarnNameKey)) {
            Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember: Method '{}' (effective: '{}') not found via Yarn mappings. Attempting DIRECT Java reflection lookup for '{}' on class {}.",
                    yarnNameKey, effectiveYarnNameKey, yarnNameKey, instanceClass.getName());
        }

        try {
            List<Method> candidateJavaMethods = ScriptUtils.findAndMakeAccessibleMethods(instanceClass, Collections.singletonList(yarnNameKey), false /*isStatic*/);

            if (isAttemptingMcExecute || !candidateJavaMethods.isEmpty()) {
                Main.LOGGER.info("[MyQOLScript-Debug] DIRECT Java reflection lookup for '{}' found {} candidate methods.", yarnNameKey, candidateJavaMethods.size());
                if(!candidateJavaMethods.isEmpty()){
                    Main.LOGGER.info("[MyQOLScript-Debug] First candidate (direct): {} (Declaring class: {})", candidateJavaMethods.get(0), candidateJavaMethods.get(0).getDeclaringClass().getName());
                }
            }

            if (!candidateJavaMethods.isEmpty()) {
                final String directMethodName = yarnNameKey;
                return (ProxyExecutable) polyglotArgs -> {
                    if (isAttemptingMcExecute) {
                        Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper: ProxyExecutable for DIRECT {}.{} (called via '{}') INVOKED.", instanceClassName, directMethodName, yarnNameKey);
                        // ... (logs des arguments)
                    }

                    List<Method> matchingOverloads = new ArrayList<>();
                    for (Method method : candidateJavaMethods) {
                        if (method.getParameterCount() == polyglotArgs.length) {
                            matchingOverloads.add(method);
                        }
                    }

                    if (matchingOverloads.isEmpty()) {
                        String availableOverloads = candidateJavaMethods.stream()
                                .map(m -> m.getName() + "(" + Arrays.stream(m.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", ")) + ") - " + m.getParameterCount() + " args")
                                .distinct().collect(Collectors.joining("; "));
                        throw new RuntimeException(
                                String.format("DIRECT: Aucune surcharge de méthode publique nommée '%s' trouvée sur %s avec %d arguments. Surcharges disponibles : [%s]",
                                        directMethodName, instanceClass.getName(), polyglotArgs.length, availableOverloads.isEmpty() ? "aucune" : availableOverloads));
                    }
                    Method methodToInvoke = matchingOverloads.get(0);
                    if (isAttemptingMcExecute) {
                        Main.LOGGER.info("[MyQOLScript-Debug] DIRECT Java reflection: Method to invoke: {}", methodToInvoke);
                    }
                    try {
                        Object[] javaArgs = ScriptUtils.unwrapArguments(polyglotArgs, methodToInvoke.getParameterTypes());
                        Object result = methodToInvoke.invoke(javaInstance, javaArgs);
                        return ScriptUtils.wrapReturnValue(result);
                    } catch (Exception e) { // Catch plus large pour logging
                        Main.LOGGER.error("[MyQOLScript-Debug] Error invoking DIRECT method {}.{}: {}", instanceClass.getName(), directMethodName, e.getMessage(), e);
                        throw new RuntimeException(String.format("Erreur lors de l'invocation DIRECTE de la méthode %s.%s : %s",
                                instanceClass.getName(), directMethodName, e.getMessage()), e);
                    }
                };
            }
        } catch (Exception e) {
            Main.LOGGER.warn("[MyQOLScript-Debug] Exception during DIRECT Java reflection lookup for method '{}' on {}: {}", yarnNameKey, instanceClass.getName(), e.getMessage(), e);
        }

        // 3. Champs mappés
        if (yarnToRuntimeFieldName.containsKey(yarnNameKey)) {
            Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.getMember: [Field Path] Found yarnNameKey '{}' in yarnToRuntimeFieldName.", yarnNameKey);
            String runtimeFieldName = yarnToRuntimeFieldName.get(yarnNameKey);
            try {
                Field field = ScriptUtils.findAndMakeAccessibleField(instanceClass, runtimeFieldName);
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new RuntimeException(String.format(
                            "Tentative d'accès au champ statique '%s' (runtime: '%s') via un wrapper d'instance de %s (Yarn: %s). Utilisez le wrapper de classe.",
                            yarnNameKey, runtimeFieldName, instanceClassName, yarnInstanceClassName));
                }
                return ScriptUtils.wrapReturnValue(field.get(javaInstance));
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(String.format("Champ d'instance '%s' (runtime: '%s') non trouvé sur l'objet de classe %s (Yarn: %s).",
                        yarnNameKey, runtimeFieldName, instanceClassName, yarnInstanceClassName), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Impossible d'accéder au champ d'instance '%s' (runtime: '%s') sur l'objet de classe %s (Yarn: %s).",
                        yarnNameKey, runtimeFieldName, instanceClassName, yarnInstanceClassName), e);
            }
        }

        Main.LOGGER.warn("[MyQOLScript-Debug] JsObjectWrapper.getMember: FALL THROUGH for key '{}' (effectiveYarnNameKey '{}'). Not found as mapped method, direct Java method, or mapped field. Returning null.", yarnNameKey, effectiveYarnNameKey);
        return null;
    }


    @Override
    public boolean hasMember(String yarnNameKey) {
        Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.hasMember for key: '{}' on instance of {} ({})", yarnNameKey, this.yarnInstanceClassName, this.instanceClassName);
        if ("_IS_MY_QOL_PROXY_".equals(yarnNameKey)) return true;

        boolean isMinecraftClientInstance = MINECRAFT_CLIENT_YARN_FQCN.equals(this.yarnInstanceClassName);
        String effectiveYarnNameKey = yarnNameKey; // Initialiser avec yarnNameKey

        // Gestion de l'alias pour 'execute'
        if ("doTheExecution".equals(yarnNameKey) && isMinecraftClientInstance) {
            effectiveYarnNameKey = "execute";
        }

        // 1. Vérifier les méthodes mappées par Yarn (en utilisant effectiveYarnNameKey)
        if (yarnToRuntimeMethodNames.containsKey(effectiveYarnNameKey)) {
            Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.hasMember for '{}' (resolved to effective '{}'): FOUND in yarnToRuntimeMethodNames.", yarnNameKey, effectiveYarnNameKey);
            return true;
        }

        // 2. Vérifier les champs mappés par Yarn (en utilisant yarnNameKey original, car les alias sont pour les méthodes)
        if (yarnToRuntimeFieldName.containsKey(yarnNameKey)) {
            Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.hasMember for '{}': FOUND in yarnToRuntimeFieldName.", yarnNameKey);
            return true;
        }

        // 3. Pour les méthodes non mappées par Yarn, si c'est "execute" sur un client MC, on suppose qu'elle existe
        //    pour que getMember puisse tenter la réflexion. Pour les autres, on pourrait être plus strict
        //    ou effectuer une recherche par réflexion ici si la performance le permet.
        if (("execute".equals(yarnNameKey) || "doTheExecution".equals(yarnNameKey)) && isMinecraftClientInstance) {
            Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.hasMember for '{}' (MCClient): ASSUMING potentially available via direct reflection. Returning true to allow getMember attempt.", yarnNameKey);
            return true;
        }

        // Optionnel: Recherche par réflexion plus générale pour hasMember si nécessaire (peut impacter les perfs)
        // try {
        //   if (!ScriptUtils.findAndMakeAccessibleMethods(instanceClass, Collections.singletonList(yarnNameKey), false).isEmpty()) {
        //     Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.hasMember for '{}': FOUND via direct reflection for non-mapped/non-execute.", yarnNameKey);
        //     return true;
        //   }
        // } catch (Exception e) { /* ignore */ }


        Main.LOGGER.info("[MyQOLScript-Debug] JsObjectWrapper.hasMember for '{}' (effective: '{}'): NOT FOUND by any check.", yarnNameKey, effectiveYarnNameKey);
        return false;
    }


    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>();
        keys.add("_IS_MY_QOL_PROXY_");

        boolean isMinecraftClientInstance = MINECRAFT_CLIENT_YARN_FQCN.equals(this.yarnInstanceClassName);
        // Ajoute l'alias si 'execute' est réellement disponible (soit par map, soit on suppose par réflexion)
        if (isMinecraftClientInstance && (yarnToRuntimeMethodNames.containsKey("execute") || true /*on suppose que execute est tjrs dispo sur MC via reflection*/)) {
            keys.add("doTheExecution");
        }
        keys.addAll(yarnToRuntimeMethodNames.keySet());
        keys.addAll(yarnToRuntimeFieldName.keySet());

        // Pour être complet, on pourrait aussi ajouter les noms de méthodes publiques trouvables par réflexion,
        // mais cela rendrait getMemberKeys coûteux. Pour l'instant, on se contente des méthodes/champs mappés et de l'alias.
        // Si "execute" n'est pas dans yarnToRuntimeMethodNames mais qu'on le gère par réflexion, on pourrait l'ajouter ici aussi.
        if (isMinecraftClientInstance && !yarnToRuntimeMethodNames.containsKey("execute")) {
            keys.add("execute"); // On s'attend à le trouver par réflexion
        }

        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String yarnNameKey, Value valueToSet) {
        // ... (code existant pour putMember, avec yarnInstanceClassName dans les logs/errors) ...
        if (yarnToRuntimeFieldName.containsKey(yarnNameKey)) {
            String runtimeFieldName = yarnToRuntimeFieldName.get(yarnNameKey);
            Field field = null;

            try {
                field = ScriptUtils.findAndMakeAccessibleField(instanceClass, runtimeFieldName);

                if (Modifier.isStatic(field.getModifiers())) {
                    throw new UnsupportedOperationException(String.format(
                            "Tentative d'assignation au champ statique '%s' (runtime: '%s') via un wrapper d'instance de %s (Yarn: %s). Utilisez le wrapper de classe.",
                            yarnNameKey, runtimeFieldName, instanceClassName, yarnInstanceClassName));
                }

                if (Modifier.isFinal(field.getModifiers())) {
                    throw new UnsupportedOperationException(String.format(
                            "Cannot modify FINAL instance field '%s' on object of class %s (Yarn: %s).",
                            yarnNameKey, instanceClassName, yarnInstanceClassName));
                }

                Object javaValue = ScriptUtils.unwrapArguments(
                        new Value[]{valueToSet},
                        new Class<?>[]{field.getType()}
                )[0];

                field.set(javaInstance, javaValue);
                return;

            } catch (NoSuchFieldException e) {
                throw new RuntimeException(String.format(
                        "Champ d'instance '%s' (runtime: '%s') non trouvé pour l'assignation sur l'objet de classe %s (Yarn: %s).",
                        yarnNameKey, runtimeFieldName, instanceClassName, yarnInstanceClassName), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format(
                        "Impossible d'assigner au champ d'instance '%s' (runtime: '%s') sur l'objet de classe %s (Yarn: %s) en raison de restrictions d'accès.",
                        yarnNameKey, runtimeFieldName, instanceClassName, yarnInstanceClassName), e);
            } catch (IllegalArgumentException e) {
                String fieldTypeName = (field != null) ? field.getType().getName() : "inconnu (champ non résolu)";
                throw new RuntimeException(String.format(
                        "Type de valeur incompatible pour l'assignation au champ d'instance '%s' (runtime: '%s', type attendu: %s) sur l'objet de classe %s (Yarn: %s).",
                        yarnNameKey, runtimeFieldName, fieldTypeName, instanceClassName, yarnInstanceClassName), e);
            } catch (RuntimeException e) { // Includes UnsupportedOperationException from static check
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "Erreur lors de la tentative d'assignation au champ d'instance '%s' (runtime: '%s') sur l'objet de classe %s (Yarn: %s): %s",
                        yarnNameKey, runtimeFieldName, instanceClassName, yarnInstanceClassName, e.getMessage()), e);
            }
        }

        throw new UnsupportedOperationException(
                String.format("Le membre '%s' n'est pas un champ modifiable connu (ou non trouvé) sur les objets de classe %s (Yarn: %s).",
                        yarnNameKey, instanceClassName, yarnInstanceClassName));
    }

    @Override
    public String toString() {
        return "JsObjectWrapper[yarnClass=" + (yarnInstanceClassName.isEmpty() ? "N/A" : yarnInstanceClassName) +
                ", runtimeClass=" + instanceClassName + "@" + Integer.toHexString(System.identityHashCode(javaInstance)) + "]";
    }
}