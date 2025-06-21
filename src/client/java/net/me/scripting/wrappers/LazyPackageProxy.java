package net.me.scripting.wrappers;

import net.me.scripting.engine.ScriptingClassResolver;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

public class LazyPackageProxy implements ProxyObject {
    private final String currentPath;
    private final ScriptingClassResolver classResolver;
    private final Map<String, Object> cache = new HashMap<>();

    public LazyPackageProxy(String currentPath, ScriptingClassResolver classResolver) {
        this.currentPath = currentPath;
        this.classResolver = classResolver;
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
        if (classResolver.isFullClassPath(path)) {
            String runtimeName = classResolver.getRuntimeName(path);
            if (runtimeName != null) {
                return new LazyJsClassHolder(path, runtimeName, classResolver);
            }
        }

        if (classResolver.isPackage(path)) {
            return new LazyPackageProxy(path, classResolver);
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
        return classResolver.isFullClassPath(nextPath) || classResolver.isPackage(nextPath);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot set members on a script package.");
    }
}