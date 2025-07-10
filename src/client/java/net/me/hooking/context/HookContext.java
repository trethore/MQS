package net.me.hooking.context;

import net.me.Main;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.HostAccess;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class HookContext {
    private final Object instance;
    private final Method method;
    private final CallerInfo caller;
    private final MappingsManager mappingsManager;

    private String yarnMethodName;
    private String yarnClassName;

    public HookContext(Object instance, Method method, CallerInfo caller, MappingsManager mappingsManager) {
        this.instance = instance;
        this.method = method;
        this.caller = caller;
        this.mappingsManager = mappingsManager;
    }

    @HostAccess.Export
    public Object getInstance() {
        return ScriptUtils.wrapReturn(instance);
    }

    @HostAccess.Export
    public CallerInfo getCaller() {
        return caller;
    }

    @HostAccess.Export
    public MethodInfo getMethod() {
        return new MethodInfo(method);
    }

    @HostAccess.Export
    public boolean isStatic() {
        return new MethodInfo(method).isStatic();
    }

    @HostAccess.Export
    public Object getMethodClass() {
        return Main.getInstance().getScriptManager().getClassResolver().getOrCreateWrapper(method.getDeclaringClass().getName());
    }

    @HostAccess.Export
    public String getYarnMethodClass() {
        if (yarnClassName == null) {
            yarnClassName = mappingsManager.getRuntimeToYarnClassMap().getOrDefault(method.getDeclaringClass().getName(), method.getDeclaringClass().getName());
        }
        return yarnClassName;
    }

    @HostAccess.Export
    public String getYarnMethod() {
        if (yarnMethodName != null) {
            return yarnMethodName;
        }

        String declaringClassYarnName = getYarnMethodClass();
        Map<String, List<String>> methodsForClass = mappingsManager.getMethodMap().get(declaringClassYarnName);

        if (methodsForClass != null) {
            for (Map.Entry<String, List<String>> entry : methodsForClass.entrySet()) {
                if (entry.getValue().contains(method.getName())) {
                    this.yarnMethodName = entry.getKey();
                    return this.yarnMethodName;
                }
            }
        }
        this.yarnMethodName = method.getName();
        return this.yarnMethodName;
    }
}