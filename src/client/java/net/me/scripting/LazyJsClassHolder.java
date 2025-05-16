package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class LazyJsClassHolder implements ProxyObject {
    private final String yarnName;
    private final String runtimeName;
    private final ScriptManager scriptManager;

    private JsClassWrapper resolvedWrapper = null;

    public LazyJsClassHolder(String yarnName, String runtimeName, ScriptManager scriptManager) {
        this.yarnName = yarnName;
        this.runtimeName = runtimeName;
        this.scriptManager = scriptManager;
    }

    public String getYarnName() {
        return yarnName;
    }

    public String getRuntimeName() {
        return runtimeName;
    }

    public JsClassWrapper resolve() {
        if (resolvedWrapper == null) {
            Main.LOGGER.debug("Lazy loading JsClassWrapper for: {} (runtime: {})", yarnName, runtimeName);
            try {
                this.resolvedWrapper = scriptManager.createActualJsClassWrapper(runtimeName);
            } catch (ClassNotFoundException e) {
                Main.LOGGER.error("Failed to lazy load class (ClassNotFoundException) for {}: {}", yarnName, e.getMessage());
                throw new RuntimeException("Lazy load failed: Class not found " + runtimeName, e);
            } catch (Throwable t) {
                Main.LOGGER.error("Failed to lazy load class for {} (runtime: {}): {}", yarnName, runtimeName, t, t);
                throw new RuntimeException("Lazy load failed for " + yarnName + ": " + t.getMessage(), t);
            }
        }
        if (resolvedWrapper == null) {
            throw new IllegalStateException("Failed to resolve JsClassWrapper for " + yarnName + " even after attempt.");
        }
        return resolvedWrapper;
    }


    @Override
    public Object getMember(String key) {
        return resolve().getMember(key);
    }

    @Override
    public Object getMemberKeys() {
        return resolve().getMemberKeys();
    }

    @Override
    public boolean hasMember(String key) {
        return resolve().hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        resolve().putMember(key, value);
    }
}