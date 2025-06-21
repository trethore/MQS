package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.module.ScriptDescriptor;

import java.util.List;

public class ScriptCommands {

    public static class ListScriptsCommand implements ConsoleCommand {
        @Override
        public void execute(String[] args) {
            ScriptManager sm = ScriptManager.getInstance();
            ConsoleManager cm = ConsoleManager.getInstance();
            cm.logInfo("--- Available Scripts ---");
            if (sm.getAvailableScripts().isEmpty()) {
                cm.logInfo("No scripts found. Add .js files to the 'my-qol-scripts/scripts' folder.");
                return;
            }
            for (ScriptDescriptor descriptor : sm.getAvailableScripts()) {
                boolean isRunning = sm.isRunning(descriptor.getId());
                String status = isRunning ? "§a[ENABLED]§r" : "§c[DISABLED]§r";
                cm.logInfo(String.format(" - %s %s", descriptor.getId(), status));
            }
        }
        @Override public String getName() { return "list"; }
        @Override public String getDescription() { return "Lists all available scripts and their status."; }
        @Override public String getUsage() { return "list"; }
    }

    public static class EnableScriptCommand implements ConsoleCommand {
        @Override
        public void execute(String[] args) {
            if (args.length == 0) {
                ConsoleManager.getInstance().logError("No script ID provided. Usage: " + getUsage());
                return;
            }
            String scriptId = String.join(" ", args);
            ScriptManager.getInstance().enableScript(scriptId);
            // The script manager already logs success/failure, so we don't need another message here.
        }
        @Override public String getName() { return "enable"; }
        @Override public String getDescription() { return "Enables a script by its ID."; }
        @Override public String getUsage() { return "enable <script_id>"; }
    }

    public static class DisableScriptCommand implements ConsoleCommand {
        @Override
        public void execute(String[] args) {
            if (args.length == 0) {
                ConsoleManager.getInstance().logError("No script ID provided. Usage: " + getUsage());
                return;
            }
            String scriptId = String.join(" ", args);
            ScriptManager.getInstance().disableScript(scriptId);
        }
        @Override public String getName() { return "disable"; }
        @Override public String getDescription() { return "Disables a running script by its ID."; }
        @Override public String getUsage() { return "disable <script_id>"; }
    }

    public static class RefreshScriptsCommand implements ConsoleCommand {
        @Override
        public void execute(String[] args) {
            ConsoleManager.getInstance().logInfo("Refreshing scripts...");
            ScriptManager.getInstance().refreshAndReenable();
            ConsoleManager.getInstance().logSuccess("Scripts refreshed and previously running scripts re-enabled.");
        }
        @Override public String getName() { return "refresh"; }
        @Override public String getDescription() { return "Refreshes and reloads all scripts from disk."; }
        @Override public String getUsage() { return "refresh"; }
    }

    public static class DisableAllCommand implements ConsoleCommand {
        @Override
        public void execute(String[] args) {
            ScriptManager sm = ScriptManager.getInstance();
            List<String> runningScriptIds = sm.getRunningScripts().stream()
                    .map(RunningScript::getId)
                    .toList();

            if (runningScriptIds.isEmpty()) {
                ConsoleManager.getInstance().logInfo("No scripts are currently running.");
                return;
            }

            runningScriptIds.forEach(sm::disableScript);
            ConsoleManager.getInstance().logSuccess("Disabled all " + runningScriptIds.size() + " running scripts.");
        }
        @Override public String getName() { return "disable-all"; }
        @Override public String getDescription() { return "Disables all currently running scripts."; }
        @Override public String getUsage() { return "disable-all"; }
    }
}