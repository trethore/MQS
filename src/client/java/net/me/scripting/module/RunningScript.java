package net.me.scripting.module;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class RunningScript {
    private final ScriptDescriptor descriptor;
    private final Value jsInstance;
    private final String name;
    private final Context context;

    public RunningScript(ScriptDescriptor descriptor, Value jsInstance, Context context) {
        this.descriptor = descriptor;
        this.jsInstance = jsInstance;
        this.context = context;

        Value nameValue = jsInstance.getMember("name");
        this.name = (nameValue != null && nameValue.isString()) ? nameValue.asString() : descriptor.moduleName();
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

    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    public String getId() {
        return descriptor.getId();
    }

    public Value getJsInstance() {
        return jsInstance;
    }

    public Context getContext() {
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