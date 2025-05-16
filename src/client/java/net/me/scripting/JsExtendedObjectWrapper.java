package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class JsExtendedObjectWrapper implements ProxyObject {
    private final Object javaInstance;
    private final Class<?> instanceClass; // The concrete class of javaInstance
    private final String instanceClassName;

    // Mappings for the original Java class (Yarn name -> Runtime name(s))
    private final Map<String, List<String>> originalYarnToRuntimeMethods;
    private final Map<String, String> originalYarnToRuntimeFields;

    private final Value jsOverrides; // The JS object { "methodName": function(){...} }
    private final SuperAccessWrapper superAccessor;

    public JsExtendedObjectWrapper(Object javaInstance,
                                   Class<?> instanceClass,
                                   Map<String, List<String>> originalMethodMappings, // Yarn -> List<Runtime>
                                   Map<String, String> originalFieldMappings,   // Yarn -> Runtime
                                   Value jsOverrides) {
        if (javaInstance == null) {
            throw new NullPointerException("Java instance cannot be null for JsExtendedObjectWrapper.");
        }
        this.javaInstance = javaInstance;
        this.instanceClass = instanceClass;
        this.instanceClassName = instanceClass.getName();
        this.originalYarnToRuntimeMethods = Collections.unmodifiableMap(new HashMap<>(originalMethodMappings));
        this.originalYarnToRuntimeFields = Collections.unmodifiableMap(new HashMap<>(originalFieldMappings));
        this.jsOverrides = jsOverrides; // This is a GraalVM Value representing the JS object

        // SuperAccessor provides access to the original Java methods/fields
        this.superAccessor = new SuperAccessWrapper(javaInstance, instanceClass, originalYarnToRuntimeMethods, originalYarnToRuntimeFields);
    }

    public Object getJavaInstance() {
        return this.javaInstance;
    }

    @Override
    public Object getMember(String yarnNameKey) {
        // 1. Check for "_super" accessor
        if ("_super".equals(yarnNameKey)) {
            return this.superAccessor;
        }

        // 2. Check for JavaScript override
        if (jsOverrides != null && jsOverrides.hasMember(yarnNameKey)) {
            Value jsMember = jsOverrides.getMember(yarnNameKey);
            if (jsMember.canExecute()) {
                // Return a ProxyExecutable that calls the JS function with `this` bound
                // to this JsExtendedObjectWrapper.
                return (ProxyExecutable) polyglotArgs -> {
                    try {
                        // To call a JS function with a specific 'this': member.invokeMember("call", thisValue, arg1, arg2, ...)
                        // We need to convert polyglotArgs (Value[]) to Object[] for invokeMember's varargs.
                        // However, Value.execute takes Value... directly for arguments.
                        // `this` in JS is tricky. If `jsMember` is `foo.bar`, GraalJS might set `this` to `foo`.
                        // We want `this` to be this `JsExtendedObjectWrapper`.
                        // The most reliable way is to use `function.call(thisArg, ...args)`
                        // or if the function is already bound, `jsMember.execute(polyglotArgs)` might work.
                        // Let's try to pass `this` JsExtendedObjectWrapper as the first argument to `call`.
                        Value[] callArgs = new Value[polyglotArgs.length + 1];
                        callArgs[0] = Value.asValue(this); // `this` for the JS function
                        System.arraycopy(polyglotArgs, 0, callArgs, 1, polyglotArgs.length);

                        Value result = jsMember.invokeMember("call", callArgs);
                        return ScriptUtils.jsValueToJavaOrProxy(result, true); // Allow further wrapping
                    } catch (PolyglotException e) {
                        Main.LOGGER.error("Error executing JS override for {}.{}: {}", instanceClassName, yarnNameKey, e.getMessage(), e);
                        throw e; // Re-throw to JS
                    }
                };
            } else {
                // JS override is a property, not a function
                return ScriptUtils.jsValueToJavaOrProxy(jsMember, true);
            }
        }

        // 3. No JS override, try original Java instance methods
        if (originalYarnToRuntimeMethods.containsKey(yarnNameKey)) {
            return createJavaMethodProxy(yarnNameKey, false /* isStatic */, originalYarnToRuntimeMethods.get(yarnNameKey), javaInstance, instanceClass, instanceClassName);
        }

        // 4. No JS override, try original Java instance fields
        if (originalYarnToRuntimeFields.containsKey(yarnNameKey)) {
            String runtimeFieldName = originalYarnToRuntimeFields.get(yarnNameKey);
            try {
                Field field = ScriptUtils.findAndMakeAccessibleField(instanceClass, runtimeFieldName);
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new RuntimeException(String.format(
                            "Attempt to access static field '%s' (runtime: '%s') via an instance wrapper of %s. Use the class wrapper.",
                            yarnNameKey, runtimeFieldName, instanceClassName));
                }
                return ScriptUtils.wrapReturnValue(field.get(javaInstance));
            } catch (NoSuchFieldException e) {
                // This might happen if the field is in a superclass not covered by initial simple mapping for the exact class
                // but ScriptUtils.findAndMakeAccessibleField should handle superclasses.
                // Or, the mapping is simply missing for this specific key.
            } catch (IllegalAccessException e) {
                throw new RuntimeException(String.format("Cannot access instance field '%s' (runtime: '%s') on %s.",
                        yarnNameKey, runtimeFieldName, instanceClassName), e);
            }
        }
        return null; // Member not found
    }

    @Override
    public boolean hasMember(String yarnNameKey) {
        if ("_super".equals(yarnNameKey)) return true;
        if (jsOverrides != null && jsOverrides.hasMember(yarnNameKey)) return true;
        return originalYarnToRuntimeMethods.containsKey(yarnNameKey) || originalYarnToRuntimeFields.containsKey(yarnNameKey);
    }

    @Override
    public Object getMemberKeys() {
        Set<String> keys = new HashSet<>();
        keys.add("_super");
        if (jsOverrides != null) {
            Value overrideKeysArray = Value.asValue(jsOverrides.getMemberKeys());
            if (overrideKeysArray != null && overrideKeysArray.hasArrayElements()) {
                for (long i = 0; i < overrideKeysArray.getArraySize(); i++) {
                    Value keyElement = overrideKeysArray.getArrayElement(i);
                    if (keyElement.isString()) {
                        keys.add(keyElement.asString());
                    }
                }
            }
        }
        keys.addAll(originalYarnToRuntimeMethods.keySet());
        keys.addAll(originalYarnToRuntimeFields.keySet());
        return keys.toArray(new String[0]);
    }

    @Override
    public void putMember(String yarnNameKey, Value value) {
        // 1. Check if trying to set on JS override object (if it supports it - for plain JS objects, this is fine)
        // This part is complex: should JS overrides be mutable through the wrapper?
        // For simplicity, let's assume overrides are defined at creation.
        // If you want to allow changing JS-defined properties:
        // if (jsOverrides != null && jsOverrides.hasMember(yarnNameKey)) {
        //     jsOverrides.putMember(yarnNameKey, value);
        //     return;
        // }

        // 2. Try to set original Java instance field
        if (originalYarnToRuntimeFields.containsKey(yarnNameKey)) {
            String runtimeFieldName = originalYarnToRuntimeFields.get(yarnNameKey);
            Field field;
            try {
                field = ScriptUtils.findAndMakeAccessibleField(instanceClass, runtimeFieldName);
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new UnsupportedOperationException("Cannot set static field '" + yarnNameKey + "' via instance. Use class wrapper.");
                }
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new UnsupportedOperationException("Cannot modify FINAL instance field '" + yarnNameKey + "'.");
                }
                Object javaValue = ScriptUtils.unwrapArguments(new Value[]{value}, new Class<?>[]{field.getType()})[0];
                field.set(javaInstance, javaValue);
                return;
            } catch (NoSuchFieldException e) {
                // Fall through to error
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access instance field '" + yarnNameKey + "' for assignment.", e);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Type mismatch for instance field '" + yarnNameKey + "'.", e);
            }
        }

        throw new UnsupportedOperationException(String.format(
                "Member '%s' not found or not settable on extended object of %s, or it's a method/JS override.",
                yarnNameKey, instanceClassName));
    }

    // Helper to create ProxyExecutable for Java methods (similar to JsObjectWrapper/JsClassWrapper)
    // Could be refactored into ScriptUtils if used by multiple wrappers
    static ProxyExecutable createJavaMethodProxy(String yarnNameKey, boolean isStatic, List<String> runtimeNames, Object targetInstance, Class<?> targetClass, String targetClassName) {
        List<Method> candidateMethods = ScriptUtils.findAndMakeAccessibleMethods(targetClass, runtimeNames, isStatic);

        return (ProxyExecutable) polyglotArgs -> {
            List<Method> matchingOverloads = new ArrayList<>();
            for (Method method : candidateMethods) {
                if (method.getParameterCount() == polyglotArgs.length) {
                    matchingOverloads.add(method);
                }
            }

            if (matchingOverloads.isEmpty()) {
                String availableOverloads = candidateMethods.stream().map(m -> m.getParameterCount() + " args").distinct().collect(Collectors.joining(", "));
                throw new RuntimeException(String.format("No overload for %s method '%s' on %s with %d args. Available for '%s': [%s]",
                        isStatic ? "static" : "instance", yarnNameKey, targetClassName, polyglotArgs.length, yarnNameKey, availableOverloads));
            }

            Method methodToInvoke = matchingOverloads.get(0); // Basic overload resolution (by arg count)
            // More sophisticated resolution might be needed for ambiguous cases

            try {
                Object[] javaArgs = ScriptUtils.unwrapArguments(polyglotArgs, methodToInvoke.getParameterTypes());
                Object result = methodToInvoke.invoke(targetInstance, javaArgs); // targetInstance is null for static
                return ScriptUtils.wrapReturnValue(result);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new RuntimeException(String.format("Error invoking %s method %s.%s (runtime: %s): %s",
                        isStatic ? "static" : "instance", targetClassName, yarnNameKey, methodToInvoke.getName(), e.getMessage()), e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(String.format("%s method %s.%s (runtime: %s) threw: %s",
                        isStatic ? "static" : "instance", targetClassName, yarnNameKey, methodToInvoke.getName(), e.getCause().getMessage()), e.getCause());
            }
        };
    }
}