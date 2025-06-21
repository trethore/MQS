package net.me.console;

public interface ConsoleCommand {
    void execute(String[] args);
    String getName();
    String getDescription();
    String getUsage();
}