package net.me.scripting;

import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;

import java.util.Collection;
import java.util.List;


public class ScriptingService {
    private final ScriptManager sm = ScriptManager.getInstance();

    public Collection<ScriptDescriptor> listAvailable() {
        return sm.getAvailableScripts();
    }

    public Collection<RunningScript> listRunning() {
        return sm.getRunningScripts();
    }

    public boolean isRunning(String scriptId) {
        return sm.isRunning(scriptId);
    }

    public void enable(String scriptId) {
        sm.enableScript(scriptId);
    }

    public void disable(String scriptId) {
        sm.disableScript(scriptId);
    }

    public int disableAll() {
        List<String> runningScriptIds = sm.getRunningScripts().stream()
                .map(RunningScript::getId)
                .toList();

        if (runningScriptIds.isEmpty()) {
            return 0;
        }

        runningScriptIds.forEach(this::disable);
        return runningScriptIds.size();
    }

    public void refresh() {
        sm.refreshAndReenable();
    }
}