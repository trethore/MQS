package net.me.scripting;

import net.me.Main;
import net.me.mappings.MappingsManager;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ScriptUtils {

    public record ClassMappings(
            Map<String, List<String>> methods,
            Map<String, String> fields
    ) {}

    public static Field findAndMakeAccessibleField(Class<?> targetClass, String runtimeFieldName) throws NoSuchFieldException {
        Class<?> currentClass = targetClass;
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(runtimeFieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                // just try the superclass
            }
            currentClass = currentClass.getSuperclass();
        }
        assert targetClass != null;
        throw new NoSuchFieldException("Field '" + runtimeFieldName + "' not found in class " + targetClass.getName() + " or its superclasses.");
    }

    public static List<Method> findAndMakeAccessibleMethods(Class<?> targetClass, List<String> runtimeMethodNames, boolean isStatic) {
        List<Method> foundMethods = new ArrayList<>();
        Class<?> currentClass = targetClass;
        while (currentClass != null) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (runtimeMethodNames.contains(method.getName()) && Modifier.isStatic(method.getModifiers()) == isStatic) {
                    try {
                        method.setAccessible(true);
                        foundMethods.add(method);
                    } catch (SecurityException e) {
                        Main.LOGGER.warn("Could not make method {} accessible due to security restrictions: {}", method, e.getMessage());
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return foundMethods;
    }

    public static Object[] unwrapArguments(Value[] polyglotArgs, Class<?>[] javaParamTypes) {
        if (polyglotArgs == null) return new Object[0];
        Object[] javaArgs = new Object[polyglotArgs.length];
        for (int i = 0; i < polyglotArgs.length; i++) {
            Value v = polyglotArgs[i];
            Class<?> expectedType = (i < javaParamTypes.length) ? javaParamTypes[i] : null;

            if (v == null || v.isNull()) {
                javaArgs[i] = null;
            } else if (v.isBoolean()) {
                javaArgs[i] = v.asBoolean();
            } else if (v.isNumber()) {
                if (expectedType != null) {
                    if (expectedType == int.class || expectedType == Integer.class) javaArgs[i] = v.asInt();
                    else if (expectedType == long.class || expectedType == Long.class) javaArgs[i] = v.asLong();
                    else if (expectedType == double.class || expectedType == Double.class) javaArgs[i] = v.asDouble();
                    else if (expectedType == float.class || expectedType == Float.class) javaArgs[i] = v.asFloat();
                    else if (expectedType == short.class || expectedType == Short.class) javaArgs[i] = v.asShort();
                    else if (expectedType == byte.class || expectedType == Byte.class) javaArgs[i] = v.asByte();
                    else javaArgs[i] = v.asDouble(); // Default for number if type unknown/unhandled
                } else {
                    javaArgs[i] = v.asDouble(); // Default for number if no type info
                }
            } else if (v.isString()) {
                javaArgs[i] = v.asString();
            } else if (v.isHostObject()) {
                javaArgs[i] = v.asHostObject();
            } else if (v.isProxyObject()) {
                Object proxied = v.asProxyObject();
                if (proxied instanceof JsObjectWrapper) {
                    javaArgs[i] = ((JsObjectWrapper) proxied).getJavaInstance();
                } else if (proxied instanceof JsExtendedObjectWrapper) { // <<< ADD THIS CASE
                    javaArgs[i] = ((JsExtendedObjectWrapper) proxied).getJavaInstance();
                } else if (proxied instanceof LazyJsClassHolder && expectedType != null && expectedType.isAssignableFrom(Class.class)) {
                    // Special case: if a LazyJsClassHolder is passed where a Class is expected,
                    // resolve it and pass the actual Class object.
                    // This is for cases like `Java.extend(MyLazyClass._class, ...)`
                    Object resolvedMember = ((LazyJsClassHolder) proxied).resolve().getMember("_class");
                    if (resolvedMember instanceof Class) {
                        javaArgs[i] = resolvedMember;
                    } else {
                        javaArgs[i] = proxied; // Fallback
                    }
                }
                // Check if the unwrapped instance is assignable to the expected type
                // This is crucial for method invocation parameter matching
                if (javaArgs[i] != null && expectedType != null && !expectedType.isAssignableFrom(javaArgs[i].getClass())) {
                    // This could happen if unwrapArguments returns a proxy when a raw type was expected
                    // and the proxy itself is not assignable.
                    // For now, we trust that getJavaInstance() gives the correct underlying type.
                    // If type mismatches persist, more sophisticated unwrapping or type checking is needed here.
                    Main.LOGGER.warn("Potential type mismatch after unwrapping proxy. Expected: {}, Got: {}. Value: {}",
                            expectedType.getName(), javaArgs[i].getClass().getName(), v);
                    // If it's a proxy, and the expected type IS the proxy type, then don't unwrap.
                    // This is complex. For now, the current logic tries to unwrap to base Java types.
                }

                // If after unwrapping JsObjectWrapper or JsExtendedObjectWrapper, javaArgs[i] is still null (which shouldn't happen)
                // or if it was another type of proxy object that we didn't explicitly unwrap,
                // we assign the proxy itself. This might be fine if the Java method expects an interface
                // that the proxy implements.
                if (javaArgs[i] == null && proxied != null) { // if unwrapping failed to assign but proxied existed
                    javaArgs[i] = proxied;
                }


            } else if (v.canExecute() && expectedType != null && expectedType.isInterface() && expectedType.isAnnotationPresent(FunctionalInterface.class)) {
                // --- THIS IS THE KEY ADDITION ---
                // If it's a JS function (v.canExecute()) and the expected Java type is a functional interface (like Runnable)
                // GraalVM can convert it.
                try {
                    javaArgs[i] = v.as(expectedType);
                    Main.LOGGER.debug("Successfully converted JS function to Java functional interface: {}", expectedType.getName());
                } catch (PolyglotException | ClassCastException e) {
                    Main.LOGGER.error("Failed to convert JS function to Java functional interface {}: {}. Falling back.",
                            expectedType.getName(), e.getMessage());
                    javaArgs[i] = v; // Fallback or throw error
                }
                // --- END OF ADDITION ---
            }  else { // For other Value types (e.g., JS arrays, raw JS functions, JS objects not proxied)
                Main.LOGGER.warn("Cannot directly convert JS value to Java for argument. Type: {}. Value: {} (Meta: {})", v.getMetaObject().getMetaSimpleName(), v, v.getMetaObject());
                // Attempt to convert to a Java array if it's a JS array and expected type is an array
                if (v.hasArrayElements() && expectedType != null && expectedType.isArray()) {
                    try {
                        // This is a basic conversion; more sophisticated array handling might be needed.
                        javaArgs[i] = v.as(expectedType);
                    } catch (PolyglotException | ClassCastException e_arr) {
                        Main.LOGGER.error("Failed to convert JS array to Java array type {}: {}", expectedType.getName(), e_arr.getMessage());
                        javaArgs[i] = v; // Fallback to passing the Value object
                    }
                } else {
                    // Try to convert to the expected Java type if possible and known
                    if (expectedType != null && !v.isHostObject() && !v.isProxyObject()) {
                        try {
                            javaArgs[i] = v.as(expectedType);
                        } catch (PolyglotException | ClassCastException e_as) {
                            Main.LOGGER.warn("Failed to convert JS Value to expected Java type {}. Trying asHostObject. Error: {}", expectedType.getName(), e_as.getMessage());
                            try { javaArgs[i] = v.asHostObject(); } // Last resort for unknown objects
                            catch (PolyglotException e_host) {
                                Main.LOGGER.error("Failed to convert value to host object as a last resort: {}", v, e_host);
                                javaArgs[i] = v; // Pass the Value object itself if all else fails
                            }
                        }
                    } else {
                        try { javaArgs[i] = v.asHostObject(); } // Last resort for unknown objects
                        catch (PolyglotException e_host) {
                            Main.LOGGER.error("Failed to convert value to host object as a last resort: {}", v, e_host);
                            javaArgs[i] = v; // Pass the Value object itself if all else fails
                        }
                    }
                }
            }
        }
        return javaArgs;
    }

    public static Object wrapReturnValue(Object javaRetVal) {
        if (javaRetVal == null || javaRetVal instanceof String || javaRetVal instanceof Number || javaRetVal instanceof Boolean || javaRetVal.getClass().isPrimitive()) {
            return javaRetVal;
        }

        Class<?> retClass = javaRetVal.getClass();
        String runtimeFqcn = retClass.getName();

        MappingsManager mm = MappingsManager.getInstance();
        Map<String, String> currentRuntimeToYarn = mm.getRuntimeToYarnClassMap();
        Map<String, Map<String, List<String>>> currentAllMethodMap = mm.getMethodMap();
        Map<String, Map<String, String>> currentAllFieldMap = mm.getFieldMap();

        String yarnFqcn = currentRuntimeToYarn.get(runtimeFqcn);
        if (yarnFqcn != null || retClass.isArray()) {
            ClassMappings combinedMappings = combineMappingsForClassAndSuperclasses(retClass, currentRuntimeToYarn, currentAllMethodMap, currentAllFieldMap);
            try {
                return new JsObjectWrapper(
                        javaRetVal,
                        retClass,
                        combinedMappings.methods,
                        combinedMappings.fields
                );
            } catch (Exception e) {
                Main.LOGGER.warn("Failed to wrap return value of type {}: {}. Falling back to raw host object.", runtimeFqcn, e.getMessage());
            }
        }
        return Value.asValue(javaRetVal);
    }

    public static void insertIntoPackageHierarchy(JsPackage rootPackage, String fullYarnName, LazyJsClassHolder classHolder) {
        String[] nameParts = fullYarnName.split("\\.");
        if (nameParts.length == 0) {
            Main.LOGGER.warn("Cannot insert class holder for empty or invalid name: {}", fullYarnName);
            return;
        }

        JsPackage currentPackage = rootPackage;
        for (int i = 0; i < nameParts.length - 1; i++) {
            String packageSegment = nameParts[i];
            Object existingMember = currentPackage.getMember(packageSegment);

            if (existingMember instanceof JsPackage) {
                currentPackage = (JsPackage) existingMember;
            } else if (existingMember == null) {
                JsPackage newPackage = new JsPackage();
                currentPackage.put(packageSegment, newPackage);
                currentPackage = newPackage;
            } else if (existingMember instanceof LazyJsClassHolder || existingMember instanceof JsClassWrapper) {
                Main.LOGGER.error("Name conflict in package structure: tried to create package segment '{}' but a class/holder already exists at this path for FQCN '{}'.", packageSegment, fullYarnName);
                return;
            } else {
                Main.LOGGER.error("Unexpected object type in package hierarchy: {} for segment '{}'", existingMember.getClass().getName(), packageSegment);
                return;
            }
        }

        String className = nameParts[nameParts.length - 1];
        if (currentPackage.hasMember(className)) {
            Main.LOGGER.warn("Class holder (or package with same name) '{}' already exists in package structure for FQCN '{}'. Skipping.", className, fullYarnName);
            return;
        }
        currentPackage.put(className, classHolder);
    }

    public static ClassMappings combineMappingsForClassAndSuperclasses(
            Class<?> cls,
            Map<String, String> runtimeToYarnMap,
            Map<String, Map<String, List<String>>> allYarnMethodsMap,
            Map<String, Map<String, String>> allYarnFieldsMap
    ) {
        Main.LOGGER.debug("[ScriptUtils.combineMappings] Starting combination for class: {} (Runtime: {})",
                runtimeToYarnMap.get(cls.getName()), cls.getName()); // Log initial class
        ClassMappings result = combineMappingsRecursive(cls, runtimeToYarnMap, allYarnMethodsMap, allYarnFieldsMap, new HashMap<>(), new HashMap<>(), new HashSet<>());
        Main.LOGGER.debug("[ScriptUtils.combineMappings] Finished combination for class: {}. Methods found: {}, Fields found: {}",
                runtimeToYarnMap.get(cls.getName()), result.methods().size(), result.fields().size());
        if ("net.minecraft.client.MinecraftClient".equals(runtimeToYarnMap.get(cls.getName()))) { // Specific log for MCClient
            Main.LOGGER.info("[ScriptUtils.combineMappings] For MinecraftClient, does combinedMethods contain 'execute'? {}", result.methods().containsKey("execute"));
            if (result.methods().containsKey("execute")) {
                Main.LOGGER.info("[ScriptUtils.combineMappings] For MinecraftClient, 'execute' maps to: {}", result.methods().get("execute"));
            } else {
                Main.LOGGER.warn("[ScriptUtils.combineMappings] For MinecraftClient, 'execute' IS MISSING from combined methods. Keys: {}", result.methods().keySet());
            }
        }
        return result;
    }

    private static ClassMappings combineMappingsRecursive(
            Class<?> cls,
            Map<String, String> runtimeToYarnMap,
            Map<String, Map<String, List<String>>> allYarnMethodsMap,
            Map<String, Map<String, String>> allYarnFieldsMap,
            Map<String, List<String>> combinedMethods, // Accumulators
            Map<String, String> combinedFields,       // Accumulators
            Set<Class<?>> visited                      // To prevent cycles and redundant work
    ) {
        if (cls == null || !visited.add(cls)) {
            // Return current state of accumulators if class is null or already visited
            return new ClassMappings(combinedMethods, combinedFields);
        }

        // 1. Add mappings from the current class (cls)
        String currentRuntimeFqcn = cls.getName();
        String currentYarnFqcn = runtimeToYarnMap.get(currentRuntimeFqcn);

        Main.LOGGER.debug("[ScriptUtils.combineRecursive] Processing class: {} (Runtime: {})", currentYarnFqcn, currentRuntimeFqcn);

        if (currentYarnFqcn != null) {
            var yarnMethodsForClass = allYarnMethodsMap.get(currentYarnFqcn);
            if (yarnMethodsForClass != null) {
                Main.LOGGER.debug("[ScriptUtils.combineRecursive]   Methods from allYarnMethodsMap for {}: {} (Adding {} methods)",
                        currentYarnFqcn, yarnMethodsForClass.keySet(), yarnMethodsForClass.size());
                yarnMethodsForClass.forEach((yarnMethodName, runtimeMethodNames) -> {
                    if (combinedMethods.putIfAbsent(yarnMethodName, new ArrayList<>(runtimeMethodNames)) == null) {
                         Main.LOGGER.debug("[ScriptUtils.combineRecursive]     Added method: {} -> {}", yarnMethodName, runtimeMethodNames);
                    } else {
                         Main.LOGGER.debug("[ScriptUtils.combineRecursive]     Skipped method (already present): {} -> {}", yarnMethodName, runtimeMethodNames);
                    }
                });
                 // Specific check for ThreadExecutor and execute
                if ("net.minecraft.util.thread.ThreadExecutor".equals(currentYarnFqcn) && yarnMethodsForClass.containsKey("execute")) {
                    Main.LOGGER.info("[ScriptUtils.combineRecursive]   >>> ThreadExecutor has 'execute': {}. It was just processed for combinedMethods.", yarnMethodsForClass.get("execute"));
                }
            } else {
                 Main.LOGGER.debug("[ScriptUtils.combineRecursive]   No direct methods in allYarnMethodsMap for {}.", currentYarnFqcn);
            }
            var yarnFieldsForClass = allYarnFieldsMap.get(currentYarnFqcn);
            if (yarnFieldsForClass != null) {
                yarnFieldsForClass.forEach(combinedFields::putIfAbsent);
            }
        } else {
            Main.LOGGER.debug("[ScriptUtils.combineRecursive]   No YARN FQCN for runtime class {}, skipping direct map lookup.", currentRuntimeFqcn);
        }


        // 2. Recursively add mappings from interfaces of cls
        for (Class<?> iface : cls.getInterfaces()) {
            combineMappingsRecursive(iface, runtimeToYarnMap, allYarnMethodsMap, allYarnFieldsMap, combinedMethods, combinedFields, visited);
        }

        // 3. Recursively add mappings from the superclass of cls
        // This ensures that superclass methods are processed *after* current class and its interfaces,
        // so putIfAbsent correctly prioritizes more specific versions.
        combineMappingsRecursive(cls.getSuperclass(), runtimeToYarnMap, allYarnMethodsMap, allYarnFieldsMap, combinedMethods, combinedFields, visited);

        // The final maps are in combinedMethods and combinedFields directly due to side effects.
        // The ClassMappings object is returned at the top level.
        return new ClassMappings(combinedMethods, combinedFields); // This will be the final result from the initial call
    }
    public static Object jsValueToJavaOrProxy(Value v, boolean allowProxy) {
        if (v == null || v.isNull()) return null;
        if (v.isBoolean()) return v.asBoolean();
        if (v.isNumber()) return v.asDouble(); // Or more specific if type info available
        if (v.isString()) return v.asString();
        if (v.isHostObject()) return v.asHostObject();
        if (allowProxy && v.isProxyObject()) return v.asProxyObject(); // e.g. JsObjectWrapper, JsExtendedObjectWrapper

        // If it's a JS array or object not fitting above,
        // it might remain a Value or you might try to convert it.
        // For now, returning the Value itself might be safest if no direct conversion.
        Main.LOGGER.warn("jsValueToJavaOrProxy: Value {} couldn't be directly converted to simple Java type or proxy, returning raw Value.", v);
        return v; // Fallback for complex JS objects, arrays, functions not proxied
    }
}
