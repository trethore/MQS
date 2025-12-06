/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me.scripting;

import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptingService {
    private final ScriptManager scriptManager;
    private final ConfigManager configManager;

    public ScriptingService(ScriptManager scriptManager, ConfigManager configManager) {
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
        if (!scriptManager.isRunning(scriptId)) {
            return;
        }
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
            return "No scripts found. Add .js files to the 'myqolscripts/scripts' folder.";
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
        Collection<RunningScript> runningScripts = scriptManager.getRunningScripts();
        if (runningScripts.isEmpty()) {
            return 0;
        }

        runningScripts.forEach(configManager::saveConfig);
        return runningScripts.size();
    }
}
