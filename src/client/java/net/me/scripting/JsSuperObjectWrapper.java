package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.List;
import java.util.Map;

public class JsSuperObjectWrapper implements ProxyObject {
    private final Object javaInstance;
    private final Map<String, List<String>> yarnToRuntimeMethods;
    private final Context scriptContext;

    private Value superProxy;

    public JsSuperObjectWrapper(Object instance, Map<String, List<String>> methodMap, Context context) {
        if (instance == null) {
            throw new NullPointerException("The Java instance cannot be null for a 'super' proxy.");
        }
        this.javaInstance = instance;
        this.yarnToRuntimeMethods = methodMap;
        this.scriptContext = context;
    }

    private Value getSuperProxy() {
        if (this.superProxy == null) {
            this.superProxy = scriptContext.eval("js", "Java.super").execute(this.javaInstance);
        }
        return this.superProxy;
    }

    @Override
    public Object getMember(String yarnKey) {
        if (!yarnToRuntimeMethods.containsKey(yarnKey)) {
            return null;
        }

        return (ProxyExecutable) polyglotArgs -> {
            List<String> runtimeNames = yarnToRuntimeMethods.get(yarnKey);
            Value superObject = getSuperProxy();
            Object[] javaArgs = ScriptUtils.unwrapArgs(polyglotArgs, null); 

            for (String runtimeName : runtimeNames) {
                if (superObject.canInvokeMember(runtimeName)) {
                    try {
                        Value result = superObject.invokeMember(runtimeName, javaArgs);

                        return ScriptUtils.wrapReturn(result.as(Object.class));
                    } catch (Exception e) {
                        
                        Main.LOGGER.trace("Super invocation for yarn key '{}' (runtime '{}') failed. Trying next overload.", yarnKey, runtimeName);
                    }
                }
            }

            throw new RuntimeException(
                    String.format("Could not find a valid super method for '%s' on superclass of %s with %d arguments.",
                            yarnKey, javaInstance.getClass().getName(), javaArgs.length));
        };
    }

    @Override
    public boolean hasMember(String key) {
        return yarnToRuntimeMethods.containsKey(key);
    }

    @Override
    public Object getMemberKeys() {
        return yarnToRuntimeMethods.keySet().toArray(new String[0]);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Member assignment on a 'super' proxy is not allowed.");
    }
}