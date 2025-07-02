package net.me.console.commands;

import net.me.Main;
import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;

public class AllowAllClassesCommand extends ConsoleCommand {

    public AllowAllClassesCommand(ConsoleManager consoleManager) {
        super(consoleManager, "allowallclasses", "Allows scripts to access all Java classes. VERY DANGEROUS.", "allowallclasses <true|false>");
    }

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = getConsoleManager();
        if (args.length != 1) {
            cm.logError("Invalid arguments. Usage: " + getUsage());
            return;
        }

        boolean enable;
        if ("true".equalsIgnoreCase(args[0])) {
            enable = true;
        } else if ("false".equalsIgnoreCase(args[0])) {
            enable = false;
        } else {
            cm.logError("Invalid argument '" + args[0] + "'. Must be 'true' or 'false'.");
            return;
        }

        Main.getGlobalConfigManager().setAllClassesAllowed(enable);
        cm.logSuccess("Allowing all classes set to " + enable + ". A script reload is required for this to take full effect.");
        if (enable) {
            cm.logError("WARNING: This is a dangerous setting. Only use scripts from trusted sources.");
        }
    }
}