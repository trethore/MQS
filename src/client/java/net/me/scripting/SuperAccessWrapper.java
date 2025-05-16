package net.me.scripting;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class SuperAccessWrapper  implements ProxyObject {
    private final Object javaInstance;
    private final Class<?> instanceClass;
    private final String instanceClassName;
    private final Map<String, List<String>> yarnToRuntimeMethods; // Yarn name -> List<Runtime name>
    private final Map<String, String> yarnToRuntimeFields;    // Yarn name -> Runtime name

    public SuperAccessWrapper(Object javaInstance,
                              Class<?> instanceClass,
                              Map<String, List<String>> methodMappings,
                              Map<String, String> fieldMappings) {
        this.javaInstance = javaInstance;
        this.instanceClass = instanceClass;
        this.instanceClassName = instanceClass.getName();
        this.yarnToRuntimeMethods = methodMappings;
        this.yarnToRuntimeFields = fieldMappings;
    }

    @Override
    public Object getMember(String yarnNameKey) {
        // Access methods
        if (yarnToRuntimeMethods.containsKey(yarnNameKey)) {
            // Use the same helper, ensuring it's for an instance method call
            return JsExtendedObjectWrapper.createJavaMethodProxy(
                    yarnNameKey, false /*isStatic*/, yarnToRuntimeMethods.get(yarnNameKey),
                    javaInstance, instanceClass, instanceClassName
            );
        }

        // Access fields
        if (yarnToRuntimeFields.containsKey(yarnNameKey)) {
            String runtimeFieldName = yarnToRuntimeFields.get(yarnNameKey);
            try {
                Field field = ScriptUtils.findAndMakeAccessibleField(instanceClass, runtimeFieldName);
                if (Modifier.isStatic(field.getModifiers())) { // Should not happen for super on instance
                    throw new RuntimeException("Super access tried to get static field " + yarnNameKey);
                }
                return ScriptUtils.wrapReturnValue(field.get(javaInstance));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(String.format("Error accessing super field '%s' (runtime: '%s') on %s: %s",
                        yarnNameKey, runtimeFieldName, instanceClassName, e.getMessage()), e);
            }
        }
        return null;
    }

    @Override
    public boolean hasMember(String yarnNameKey) {
        return yarnToRuntimeMethods.containsKey(yarnNameKey) || yarnToRuntimeFields.containsKey(yarnNameKey);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>(yarnToRuntimeMethods.keySet());
        keys.addAll(yarnToRuntimeFields.keySet());
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String yarnNameKey, Value value) {
        // Generally, super properties are not set, but if needed:
        if (yarnToRuntimeFields.containsKey(yarnNameKey)) {
            String runtimeFieldName = yarnToRuntimeFields.get(yarnNameKey);
            try {
                Field field = ScriptUtils.findAndMakeAccessibleField(instanceClass, runtimeFieldName);
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new UnsupportedOperationException("Super access cannot set static field " + yarnNameKey);
                }
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new UnsupportedOperationException("Cannot modify FINAL super field '" + yarnNameKey + "'.");
                }
                Object javaValue = ScriptUtils.unwrapArguments(new Value[]{value}, new Class<?>[]{field.getType()})[0];
                field.set(javaInstance, javaValue);
                return;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(String.format("Error setting super field '%s': %s", yarnNameKey, e.getMessage()), e);
            }
        }
        throw new UnsupportedOperationException("Cannot set member '" + yarnNameKey + "' via super accessor or member not found.");
    }
}