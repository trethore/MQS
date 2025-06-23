package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.scripting.ScriptingService;
import net.me.scripting.module.ScriptDescriptor;

public class ScriptCommands {

    public static class ListScriptsCommand implements ConsoleCommand {
        private final ScriptingService scriptingService = ScriptingService.getInstance();

        @Override
        public void execute(String[] args) {
            ConsoleManager cm = ConsoleManager.getInstance();
            cm.logInfo("--- Available Scripts ---");

            if (scriptingService.listAvailable().isEmpty()) {
                cm.logInfo("No scripts found. Add .js files to the 'my-qol-scripts/scripts' folder.");
                return;
            }
            for (ScriptDescriptor descriptor : scriptingService.listAvailable()) {
                boolean isRunning = scriptingService.isRunning(descriptor.getId());
                String status = isRunning ? "§a[ENABLED]§r" : "§c[DISABLED]§r";
                cm.logInfo(String.format(" - %s %s", descriptor.getId(), status));
            }
        }

        @Override
        public String getName() {
            return "list";
        }

        @Override
        public String getDescription() {
            return "Lists all available scripts and their status.";
        }

        @Override
        public String getUsage() {
            return "list";
        }
    }

    public static class EnableScriptCommand implements ConsoleCommand {
        private final ScriptingService scriptingService = ScriptingService.getInstance();

        @Override
        public void execute(String[] args) {
            if (args.length == 0) {
                ConsoleManager.getInstance().logError("No script ID provided. Usage: " + getUsage());
                return;
            }
            String scriptId = String.join(" ", args);
            scriptingService.enable(scriptId);
        }

        @Override
        public String getName() {
            return "enable";
        }

        @Override
        public String getDescription() {
            return "Enables a script by its ID.";
        }

        @Override
        public String getUsage() {
            return "enable <script_id>";
        }
    }

    public static class DisableScriptCommand implements ConsoleCommand {
        private final ScriptingService scriptingService = ScriptingService.getInstance();

        @Override
        public void execute(String[] args) {
            if (args.length == 0) {
                ConsoleManager.getInstance().logError("No script ID provided. Usage: " + getUsage());
                return;
            }
            String scriptId = String.join(" ", args);
            scriptingService.disable(scriptId);
        }

        @Override
        public String getName() {
            return "disable";
        }

        @Override
        public String getDescription() {
            return "Disables a running script by its ID.";
        }

        @Override
        public String getUsage() {
            return "disable <script_id>";
        }
    }

    public static class RefreshScriptsCommand implements ConsoleCommand {
        private final ScriptingService scriptingService = ScriptingService.getInstance();


        @Override
        public void execute(String[] args) {
            ConsoleManager.getInstance().logInfo("Refreshing scripts...");
            scriptingService.refresh();
            ConsoleManager.getInstance().logSuccess("Scripts refreshed. All scripts are now disabled.");
        }


        @Override
        public String getName() {
            return "refresh";
        }

        @Override
        public String getDescription() {
            return "Refreshes and reloads all scripts from disk, disabling all running scripts.";
        }

        @Override
        public String getUsage() {
            return "refresh";
        }
    }

    public static class RefreshAndReenableCommand implements ConsoleCommand {
        private final ScriptingService scriptingService = ScriptingService.getInstance();

        @Override
        public void execute(String[] args) {
            ConsoleManager.getInstance().logInfo("Refreshing scripts and re-enabling...");
            scriptingService.refreshAndReenable();
            ConsoleManager.getInstance().logSuccess("Scripts refreshed and previously running scripts re-enabled.");
        }

        @Override
        public String getName() {
            return "refreshandreenable";
        }

        @Override
        public String getDescription() {
            return "Refreshes and reloads all scripts, then re-enables previously running scripts.";
        }

        @Override
        public String getUsage() {
            return "refreshandreenable";
        }
    }

    public static class DisableAllCommand implements ConsoleCommand {
        private final ScriptingService scriptingService = ScriptingService.getInstance();

        @Override
        public void execute(String[] args) {
            int disabledCount = scriptingService.disableAll();

            if (disabledCount == 0) {
                ConsoleManager.getInstance().logInfo("No scripts are currently running.");
            } else {
                ConsoleManager.getInstance().logSuccess("Disabled all " + disabledCount + " running scripts.");
            }
        }

        @Override
        public String getName() {
            return "disableall";
        }

        @Override
        public String getDescription() {
            return "Disables all currently running scripts.";
        }

        @Override
        public String getUsage() {
            return "disableall";
        }
    }
}