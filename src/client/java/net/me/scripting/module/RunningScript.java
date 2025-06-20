package net.me.scripting.module;

import net.me.Main;
import org.graalvm.polyglot.Value;

public class RunningScript {
    private final ScriptDescriptor descriptor;
    private final Value jsInstance;
    private final String name;
    private final String version;

    public RunningScript(ScriptDescriptor descriptor, Value jsInstance) {
        this.descriptor = descriptor;
        this.jsInstance = jsInstance;

        Value nameValue = jsInstance.getMember("name");
        this.name = (nameValue != null && nameValue.isString()) ? nameValue.asString() : descriptor.moduleName();
        Value versionValue = jsInstance.getMember("version");
        this.version = (versionValue != null && versionValue.isString()) ? versionValue.asString() : "N/A";
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

    public String getId() {
        return descriptor.getId();
    }

    public Value getJsInstance() {
        return jsInstance;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ScriptDescriptor getDescriptor() {
        return descriptor;
    }
}