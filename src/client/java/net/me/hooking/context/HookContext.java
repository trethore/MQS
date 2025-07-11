package net.me.hooking.context;

import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.HostAccess;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class HookContext {
    private final Object instance;
    private final Method method;
    private final StackWalker stackWalker;
    private final MappingsManager mappingsManager;
    private final ScriptManager scriptManager;

    private String yarnMethodName;
    private String yarnClassName;

    public HookContext(Object instance, Method method, StackWalker stackWalker, MappingsManager mappingsManager, ScriptManager scriptManager) {
        this.instance = instance;
        this.method = method;
        this.stackWalker = stackWalker;
        this.mappingsManager = mappingsManager;
        this.scriptManager = scriptManager;
    }

    @HostAccess.Export
    public Object getInstance() {
        return ScriptUtils.wrapReturn(instance, mappingsManager, scriptManager);
    }

    @HostAccess.Export
    public List<CallerInfo> getCallers(int depth) {
        if (depth <= 0) {
            return Collections.emptyList();
        }
        return this.stackWalker.walk(frames -> frames
                .skip(3)
                .limit(depth)
                .map(frame -> new CallerInfo(frame.toStackTraceElement()))
                .collect(Collectors.toList()));
    }

    @HostAccess.Export
    public CallerInfo getCaller() {
        List<CallerInfo> singleCallerList = getCallers(1);
        return (singleCallerList != null && !singleCallerList.isEmpty()) ? singleCallerList.getFirst() : null;
    }

    @HostAccess.Export
    public MethodInfo getMethod() {
        return new MethodInfo(method, mappingsManager, scriptManager);
    }

    @HostAccess.Export
    public boolean isStatic() {
        return new MethodInfo(method, mappingsManager, scriptManager).isStatic();
    }

    @HostAccess.Export
    public Object getMethodClass() {
        return scriptManager.getClassResolver().getOrCreateWrapper(method.getDeclaringClass().getName());
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