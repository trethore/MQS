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
        return (ProxyExecutable) polyglotArgs -> {
            Value superObject = getSuperProxy();
            Object[] javaArgs = ScriptUtils.unwrapArgs(polyglotArgs, null);

            List<String> runtimeNames = yarnToRuntimeMethods.get(yarnKey);
            if (runtimeNames != null) {
                for (String runtimeName : runtimeNames) {
                    if (superObject.canInvokeMember(runtimeName)) {
                        try {
                            Value result = superObject.invokeMember(runtimeName, javaArgs);
                            Object rawResult = result.as(Object.class);
                            return ScriptUtils.wrapReturn(rawResult);
                        } catch (Exception e) {
                            Main.LOGGER.trace("Super invocation for mapped yarn key '{}' (runtime '{}') failed. Trying next.", yarnKey, runtimeName, e);
                        }
                    }
                }
            }

            if (superObject.canInvokeMember(yarnKey)) {
                try {
                    Value result = superObject.invokeMember(yarnKey, javaArgs);
                    Object rawResult = result.as(Object.class);
                    return ScriptUtils.wrapReturn(rawResult);
                } catch (Exception e) {
                    throw new RuntimeException(
                            String.format("Super method invocation for '%s' failed even with a direct call.", yarnKey), e);
                }
            }

            throw new RuntimeException(
                    String.format("Could not find a valid super method for '%s' on superclass of %s with %d arguments.",
                            yarnKey, javaInstance.getClass().getName(), javaArgs.length));
        };
    }

    @Override
    public boolean hasMember(String key) {
        if (yarnToRuntimeMethods.containsKey(key)) {
            return true;
        }

        try {
            return getSuperProxy().canInvokeMember(key);
        } catch (Exception e) {
            Main.LOGGER.warn("Failed to check for super member '{}' on {} due to: {}", key, javaInstance.getClass().getName(), e.getMessage());
            return false;
        }
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