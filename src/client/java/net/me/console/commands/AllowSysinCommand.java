package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;

public class AllowSysinCommand implements ConsoleCommand {
    @Override
    public void execute(String[] args) {
        ConsoleManager cm = ConsoleManager.getInstance();
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

        cm.setRedirectSystemStreams(enable);
        cm.logSuccess("System stream redirection " + (enable ? "enabled" : "disabled") + ".");
    }

    @Override
    public String getName() {
        return "allowsysin";
    }

    @Override
    public String getDescription() {
        return "Redirects System.out and System.err to this console.";
    }

    @Override
    public String getUsage() {
        return "allowsysin <true|false>";
    }
}