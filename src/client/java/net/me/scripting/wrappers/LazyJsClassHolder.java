package net.me.scripting.wrappers;

import net.me.Main;
import net.me.scripting.engine.ScriptingClassResolver;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

public class LazyJsClassHolder implements ProxyObject, ProxyInstantiable {
    private final String yarnName;
    private final String runtimeName;
    private final ScriptingClassResolver classResolver;

    private JsClassWrapper resolvedWrapper;

    public LazyJsClassHolder(String yarnName, String runtimeName, ScriptingClassResolver classResolver) {
        this.yarnName = yarnName;
        this.runtimeName = runtimeName;
        this.classResolver = classResolver;
    }

    public JsClassWrapper getWrapper() {
        if (resolvedWrapper == null) {
            Main.LOGGER.debug("Lazy loading JsClassWrapper for {} -> {}", yarnName, runtimeName);
            try {
                resolvedWrapper = classResolver.createActualJsClassWrapper(runtimeName);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to lazy-load {}: {}", runtimeName, e.getMessage(), e);
                throw new RuntimeException("Cannot load class " + yarnName, e);
            }
        }
        return resolvedWrapper;
    }

    @Override
    public Object newInstance(Value... args) {
        return getWrapper().newInstance(args);
    }

    @Override
    public Object getMember(String key) {
        return getWrapper().getMember(key);
    }

    @Override
    public boolean hasMember(String key) {
        return getWrapper().hasMember(key);
    }

    @Override
    public Object getMemberKeys() {
        return getWrapper().getMemberKeys();
    }

    @Override
    public void putMember(String key, Value value) {
        getWrapper().putMember(key, value);
    }

}