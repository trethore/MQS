package net.me.hooking.context;

import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.HostAccess;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@SuppressWarnings("unused")
public class MethodInfo {
    private final Method method;

    public MethodInfo(Method method) {
        this.method = method;
    }

    @HostAccess.Export
    public String getName() {
        return method.getName();
    }

    @HostAccess.Export
    public Object getReturnType() {
        return ScriptUtils.wrapReturn(method.getReturnType());
    }

    @HostAccess.Export
    public Object[] getParameterTypes() {
        Class<?>[] params = method.getParameterTypes();
        Object[] wrappedParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            wrappedParams[i] = ScriptUtils.wrapReturn(params[i]);
        }
        return wrappedParams;
    }

    @HostAccess.Export
    public boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }
}