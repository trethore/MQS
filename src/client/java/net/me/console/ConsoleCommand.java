package net.me.console;

public abstract class ConsoleCommand {
    private final ConsoleManager consoleManager;
    private final String name;
    private final String description;
    private final String usage;


    public ConsoleCommand(ConsoleManager consoleManager, String name, String description, String usage) {
        this.consoleManager = consoleManager;
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    public abstract void execute(String[] args);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public ConsoleManager getConsoleManager() {
        return consoleManager;
    }
}