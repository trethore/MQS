package net.me.scripting.js;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import net.me.scripting.MappingData;
import net.me.scripting.ScriptManager;

import java.lang.reflect.Constructor;
import java.util.Arrays;

public class JsConstructorProxy implements ProxyExecutable {
    private final Class<?> officialClass;
    private final MappingData mappingData;
    private final ScriptManager scriptManager;

    public JsConstructorProxy(Class<?> officialClass, MappingData mappingData, ScriptManager scriptManager) {
        this.officialClass = officialClass;
        this.mappingData = mappingData;
        this.scriptManager = scriptManager;
    }

    @Override
    public Object execute(Value... arguments) {
        // Simplified: find first constructor matching arg count. Proper overload resolution needed.
        Object[] javaArgs = JsUtils.graalToJavaArgs(arguments, null, scriptManager); // null types for now

        for (Constructor<?> ctor : officialClass.getConstructors()) {
            if (ctor.getParameterCount() == arguments.length) {
                 // More robust: check assignability of JsUtils.graalToJava(arguments[i], ctor.getParameterTypes()[i])
                boolean paramsMatch = true; // Assume match for simplicity
                Class<?>[] ctorParamTypes = ctor.getParameterTypes();
                if (arguments.length > 0 && ctorParamTypes.length == arguments.length) {
                    for (int i = 0; i < arguments.length; i++) {
                        // This check needs to be more sophisticated with type conversion
                        Object convertedArg = JsUtils.graalToJava(arguments[i], ctorParamTypes[i], scriptManager);
                        if (convertedArg != null && !JsUtils.isAssignable(ctorParamTypes[i], convertedArg.getClass())) {
                             // Try number conversion
                            if (Number.class.isAssignableFrom(ctorParamTypes[i]) && convertedArg instanceof Number) {
                                javaArgs[i] = JsUtils.convertNumber((Number)convertedArg, ctorParamTypes[i]);
                                if (!JsUtils.isAssignable(ctorParamTypes[i], javaArgs[i].getClass())) {
                                    paramsMatch = false; break;
                                }
                            } else {
                                paramsMatch = false; break;
                            }
                        } else if (convertedArg == null && ctorParamTypes[i].isPrimitive()) {
                            paramsMatch = false; break; // Cannot assign null to primitive
                        } else {
                            javaArgs[i] = convertedArg; // Update with potentially converted type
                        }
                    }
                }


                if (paramsMatch) {
                    try {
                        Object instance = ctor.newInstance(javaArgs);
                        // Determine the Yarn class name of the *actual* instance type,
                        // as it might be a subclass of `officialClass` if `officialClass` is not final.
                        String instanceYarnClassName = mappingData.getMappingTree()
                                .mapClassName(instance.getClass().getName().replace('.', '/'),
                                              mappingData.getOfficialNsId(),
                                              mappingData.getNamedNsId());
                        if (instanceYarnClassName == null) {
                            instanceYarnClassName = instance.getClass().getName(); // Fallback
                        } else {
                            instanceYarnClassName = instanceYarnClassName.replace('/', '.');
                        }

                        return new JsInstanceProxy(instance, instanceYarnClassName, mappingData, scriptManager);
                    } catch (Exception e) {
                        throw new RuntimeException("Error instantiating " + officialClass.getName(), e);
                    }
                }
            }
        }
        throw new RuntimeException("No matching constructor found for " + officialClass.getName() + " with args " + Arrays.toString(arguments));
    }
}