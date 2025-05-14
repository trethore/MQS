package net.me.scripting.js;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import net.me.scripting.ScriptManager; // Assuming ScriptManager will be in this package or accessible

import java.lang.reflect.Array;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class JsUtils {

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<>();
    static {
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(void.class, Void.class); // For return types mainly
    }

    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        if (clazz == null) return false;
        return clazz.isPrimitive() || PRIMITIVE_TO_WRAPPER.containsValue(clazz);
    }

    public static boolean isGraalProxy(Object obj) {
        return obj instanceof ProxyObject ||
               obj instanceof ProxyExecutable || // Added ProxyExecutable
               obj instanceof ProxyArray ||
               obj instanceof ProxyDate ||
               obj instanceof ProxyDuration ||
               obj instanceof ProxyInstant ||
               obj instanceof ProxyIterable ||
               obj instanceof ProxyIterator ||
               obj instanceof ProxyTimeZone;
    }

    // Converts GraalJS Value to a suitable Java Object for reflection
    public static Object graalToJava(Value value, Class<?> expectedType, ScriptManager scriptManager) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isHostObject()) {
            Object hostObj = value.asHostObject();
            if (hostObj instanceof JsInstanceProxy) { // Unwrap our own proxies
                return ((JsInstanceProxy) hostObj).getJavaInstance();
            }
            return hostObj;
        }
        if (value.isString()) return value.asString();
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) {
            if (expectedType == byte.class || expectedType == Byte.class) return value.asByte();
            if (expectedType == short.class || expectedType == Short.class) return value.asShort();
            if (expectedType == int.class || expectedType == Integer.class) return value.asInt();
            if (expectedType == long.class || expectedType == Long.class) return value.asLong();
            if (expectedType == float.class || expectedType == Float.class) return value.asFloat();
            if (expectedType == double.class || expectedType == Double.class) return value.asDouble();
            return value.asDouble(); // Default
        }
        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < value.getArraySize(); i++) {
                Class<?> componentType = expectedType != null && expectedType.isArray() ? expectedType.getComponentType() : Object.class;
                list.add(graalToJava(value.getArrayElement(i), componentType, scriptManager));
            }
            if (expectedType != null && expectedType.isArray()) {
                Object array = Array.newInstance(expectedType.getComponentType(), list.size());
                for (int i = 0; i < list.size(); i++) {
                    Array.set(array, i, list.get(i));
                }
                return array;
            }
            return list; // Or convert to specific array type if expectedType is known
        }
         if (value.isDate()) return LocalDate.from(value.asDate());
        if (value.isTime()) return LocalTime.from(value.asTime());
        if (value.isInstant()) return Instant.from(value.asInstant());
        if (value.isDuration()) return Duration.from(value.asDuration());

        // For JS functions passed to Java functional interfaces
        if (value.canExecute() && expectedType != null && expectedType.isInterface() && expectedType.isAnnotationPresent(FunctionalInterface.class)) {
            return value.as(expectedType);
        }

        // Fallback for complex JS objects - could attempt to map to a Map or a POJO
        // This part is highly dependent on specific needs.
        if (value.hasMembers() && (expectedType == null || Map.class.isAssignableFrom(expectedType))) {
            Map<String, Object> map = new HashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, graalToJava(value.getMember(key), null, scriptManager)); // No expected type for map values
            }
            return map;
        }

        scriptManager.getLogger().warn("Unsupported Graal value to Java conversion: {} for type {}", value, expectedType);
        return value; // Could throw, or return the Value itself
    }

    public static Object[] graalToJavaArgs(Value[] arguments, Class<?>[] parameterTypes, ScriptManager scriptManager) {
        Object[] javaArgs = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            Class<?> expectedType = (parameterTypes != null && i < parameterTypes.length) ? parameterTypes[i] : null;
            javaArgs[i] = graalToJava(arguments[i], expectedType, scriptManager);
        }
        return javaArgs;
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertArray(Object[] objectArray, Class<T> targetArrayType) {
        if (!targetArrayType.isArray()) {
            throw new IllegalArgumentException("Target type is not an array: " + targetArrayType);
        }
        Class<?> componentType = targetArrayType.getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(componentType, objectArray.length);
        for (int i = 0; i < objectArray.length; i++) {
            java.lang.reflect.Array.set(newArray, i, convertToComponentType(objectArray[i], componentType));
        }
        return (T) newArray;
    }

    private static Object convertToComponentType(Object value, Class<?> componentType) {
        if (value == null) {
            return null; // Or throw if componentType is primitive
        }
        if (componentType.isPrimitive()) {
            if (componentType == int.class) return ((Number) value).intValue();
            if (componentType == long.class) return ((Number) value).longValue();
            // Add other primitives as needed
        }
        return componentType.cast(value);
    }

     public static boolean isAssignable(Class<?> to, Class<?> from) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (to.isPrimitive()) {
            Class<?> wrapper = PRIMITIVE_TO_WRAPPER.get(to);
            return wrapper != null && wrapper.isAssignableFrom(from);
        }
        if (from.isPrimitive()) {
             Class<?> wrapper = PRIMITIVE_TO_WRAPPER.get(from);
             return wrapper != null && to.isAssignableFrom(wrapper);
        }
        return false;
    }

    public static Number convertNumber(Number number, Class<?> targetType) {
        if (targetType == Integer.class || targetType == int.class) return number.intValue();
        if (targetType == Long.class || targetType == long.class) return number.longValue();
        if (targetType == Float.class || targetType == float.class) return number.floatValue();
        if (targetType == Double.class || targetType == double.class) return number.doubleValue();
        if (targetType == Short.class || targetType == short.class) return number.shortValue();
        if (targetType == Byte.class || targetType == byte.class) return number.byteValue();
        return number; // No conversion or unknown target
    }

    public static Value javaToGraal(Object javaObject, ScriptManager scriptManager) {
        if (javaObject == null) {
            return scriptManager.getJsContext().asValue(null);
        }
        // If it's a primitive, wrapper, or String, GraalJS handles it well.
        if (isPrimitiveOrWrapper(javaObject.getClass()) || javaObject instanceof String) {
            return scriptManager.getJsContext().asValue(javaObject);
        }
        // If it's already a Graal Value or one of our proxies, return as is (or unwrap Graal Value)
        if (javaObject instanceof Value) {
             return (Value) javaObject;
        }
        if (isGraalProxy(javaObject)) { // Our proxies are already host objects in JS terms
            return scriptManager.getJsContext().asValue(javaObject);
        }

        // For other Minecraft/Java objects, wrap them in JsInstanceProxy
        // We need to find its Yarn class name if possible.
        // This is a simplification; proper reverse mapping official->yarn is needed.
        // For now, we'll use the official name as a placeholder if no direct Yarn mapping is found.
        String yarnName = scriptManager.getMappingParser().getMappingTree()
                .mapClassName(javaObject.getClass().getName().replace('.', '/'),
                        scriptManager.getMappingParser().getOfficialNsId(),
                        scriptManager.getMappingParser().getNamedNsId());
        if(yarnName == null) yarnName = javaObject.getClass().getName();
        else yarnName = yarnName.replace('/', '.');

        return scriptManager.getJsContext().asValue(new JsInstanceProxy(javaObject, yarnName, scriptManager.getMappingParser(), scriptManager));
    }
}