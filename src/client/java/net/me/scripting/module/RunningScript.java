package net.me.scripting.module;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class RunningScript {
    private final ScriptDescriptor descriptor;
    private Value jsInstance;
    private final String name;
    private Context context;

    public RunningScript(ScriptDescriptor descriptor, Value jsInstance, Context context) {
        this.descriptor = descriptor;
        this.jsInstance = jsInstance;
        this.context = context;
        this.name = descriptor.moduleName();
    }

    public void onEnable() {
        if (jsInstance.hasMember("onEnable")) {
            try {
                jsInstance.getMember("onEnable").execute();
            } catch (Exception e) {
                Main.LOGGER.error("Error enabling script '{}': {}", name, e.getMessage(), e);
            }
        }
    }

    public void onDisable() {
        if (jsInstance.hasMember("onDisable")) {
            try {
                jsInstance.getMember("onDisable").execute();
            } catch (Exception e) {
                Main.LOGGER.error("Error disabling script '{}': {}", name, e.getMessage(), e);
            }
        }
    }


    public void invalidate() {
        this.context = null;
        this.jsInstance = null;
    }

    private void ensureValid() {
        if (context == null) {
            throw new IllegalStateException("Attempted to use a script that has been disabled and its context recycled.");
        }
    }

    public String getId() {
        return descriptor.getId();
    }

    public Value getJsInstance() {
        ensureValid();
        return jsInstance;
    }

    public Context getContext() {
        ensureValid();
        return context;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return descriptor.version();
    }

    public ScriptDescriptor getDescriptor() {
        return descriptor;
    }
}