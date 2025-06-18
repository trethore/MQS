package net.me.scripting.wrappers;

import net.me.scripting.ScriptManager;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

public class LazyPackageProxy implements ProxyObject {
    private final String currentPath;
    private final ScriptManager scriptManager;
    private final Map<String, Object> cache = new HashMap<>();

    public LazyPackageProxy(String currentPath, ScriptManager scriptManager) {
        this.currentPath = currentPath;
        this.scriptManager = scriptManager;
    }

    @Override
    public Object getMember(String key) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        String nextPath = currentPath.isEmpty() ? key : currentPath + "." + key;
        Object result = resolvePath(nextPath);
        if (result != null) {
            cache.put(key, result);
        }
        return result;
    }

    private Object resolvePath(String path) {
        if (scriptManager.isFullClassPath(path)) {
            String runtimeName = scriptManager.getRuntimeName(path);
            if (runtimeName != null) {
                return new LazyJsClassHolder(path, runtimeName, scriptManager);
            }
        }

        if (scriptManager.isPackage(path)) {
            return new LazyPackageProxy(path, scriptManager);
        }

        return null;
    }

    @Override
    public Object getMemberKeys() {
        return new String[0];
    }

    @Override
    public boolean hasMember(String key) {
        if (cache.containsKey(key)) {
            return true;
        }
        String nextPath = currentPath.isEmpty() ? key : currentPath + "." + key;
        return scriptManager.isFullClassPath(nextPath) || scriptManager.isPackage(nextPath);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot set members on a script package.");
    }
}