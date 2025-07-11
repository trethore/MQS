package net.me.console.commands;

import net.me.config.GlobalConfigManager;
import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.console.ConsoleUtils;

import java.util.Optional;

public class AllowAllClassesCommand extends ConsoleCommand {
    private final GlobalConfigManager globalConfigManager;

    public AllowAllClassesCommand(ConsoleManager consoleManager, GlobalConfigManager globalConfigManager) {
        super(consoleManager, "allowallclasses", "Allows scripts to access all Java classes. VERY DANGEROUS.", "allowallclasses <true|false>");
        this.globalConfigManager = globalConfigManager;
    }

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = getConsoleManager();
        if (args.length != 1) {
            cm.logError("Invalid arguments. Usage: " + getUsage());
            return;
        }

        Optional<Boolean> enableOpt = ConsoleUtils.parseBooleanArg(args[0]);

        if (enableOpt.isEmpty()) {
            cm.logError("Invalid argument '" + args[0] + "'. Must be '" + ConsoleUtils.TRUE_STRING + "' or '" + ConsoleUtils.FALSE_STRING + "'.");
            return;
        }

        boolean enable = enableOpt.get();
        globalConfigManager.setAllClassesAllowed(enable);
        cm.logSuccess("Allowing all classes set to " + enable + ". A script reload is required for this to take full effect.");
        if (enable) {
            cm.logError("WARNING: This is a dangerous setting. Only use scripts from trusted sources.");
        }
    }
}