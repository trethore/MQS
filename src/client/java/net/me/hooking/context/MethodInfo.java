package net.me.hooking.context;

import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.HostAccess;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@SuppressWarnings("unused")
public class MethodInfo {
    private final Method method;
    private final MappingsManager mappingsManager;
    private final ScriptManager scriptManager;

    public MethodInfo(Method method, MappingsManager mappingsManager, ScriptManager scriptManager) {
        this.method = method;
        this.mappingsManager = mappingsManager;
        this.scriptManager = scriptManager;
    }

    @HostAccess.Export
    public String getName() {
        return method.getName();
    }

    @HostAccess.Export
    public Object getReturnType() {
        return ScriptUtils.wrapReturn(method.getReturnType(), mappingsManager, scriptManager);
    }

    @HostAccess.Export
    public Object[] getParameterTypes() {
        Class<?>[] params = method.getParameterTypes();
        Object[] wrappedParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            wrappedParams[i] = ScriptUtils.wrapReturn(params[i], mappingsManager, scriptManager);
        }
        return wrappedParams;
    }

    @HostAccess.Export
    public boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }
}