package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;

public class LogRedirectCommand extends ConsoleCommand {

    public LogRedirectCommand(ConsoleManager consoleManager) {
        super(consoleManager, "logredirect", "Redirects System.out, System.err, and SLF4J logs to this console.", "logredirect <true|false>");
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

        cm.setLogRedirect(enable);
        cm.logSuccess("Log redirection " + (enable ? "enabled" : "disabled") + ".");
    }
}