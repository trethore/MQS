package net.me.scripting;

import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class ScriptingService {
    private ScriptManager scriptManager;
    private ConfigManager configManager;

    public void init(ScriptManager scriptManager, ConfigManager configManager) {
        this.scriptManager = scriptManager;
        this.configManager = configManager;
    }

    public Collection<ScriptDescriptor> listAvailable() {
        return scriptManager.getAvailableScripts();
    }

    public Collection<RunningScript> listRunning() {
        return scriptManager.getRunningScripts();
    }

    public boolean isRunning(String scriptId) {
        return scriptManager.isRunning(scriptId);
    }

    public void enable(String scriptId) {
        scriptManager.enableScript(scriptId);
    }

    public void disable(String scriptId) {
        scriptManager.disableScript(scriptId);
    }

    public int disableAll() {
        List<String> runningScriptIds = scriptManager.getRunningScripts().stream()
                .map(RunningScript::getId)
                .toList();

        if (runningScriptIds.isEmpty()) {
            return 0;
        }

        runningScriptIds.forEach(this::disable);
        return runningScriptIds.size();
    }

    public String getFormattedScriptList() {
        Collection<ScriptDescriptor> available = listAvailable();
        if (available.isEmpty()) {
            return "No scripts found. Add .js files to the 'my-qol-scripts/scripts' folder.";
        }

        return available.stream()
                .map(descriptor -> {
                    boolean isRunning = isRunning(descriptor.getId());
                    String status = isRunning ? "§a[ENABLED]" : "§c[DISABLED]";
                    return String.format(" - %s (%s) %s", descriptor.moduleName(), descriptor.getId(), status);
                })
                .collect(Collectors.joining("\n"));
    }

    public void refreshAndReenable() {
        scriptManager.refreshAndReenable();
    }

    public void refresh() {
        scriptManager.refresh();
    }

    public boolean save(String scriptId) {
        RunningScript scriptToSave = scriptManager.getRunningScripts().stream()
                .filter(script -> script.getId().equals(scriptId))
                .findFirst()
                .orElse(null);

        if (scriptToSave != null) {
            configManager.saveConfig(scriptToSave);
            return true;
        }
        return false;
    }

    public int saveAll() {
        return configManager.saveAllConfigs(scriptManager.getRunningScripts());
    }
}