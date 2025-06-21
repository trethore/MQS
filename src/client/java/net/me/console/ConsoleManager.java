package net.me.console;

import net.me.console.commands.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConsoleManager {
    private static final ConsoleManager INSTANCE = new ConsoleManager();
    private final List<ConsoleMessage> messages = new CopyOnWriteArrayList<>();
    private final Map<String, ConsoleCommand> commands = new HashMap<>();
    private final List<String> commandHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100;

    private ConsoleManager() {}

    public static ConsoleManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        registerCommands();
        logSuccess("Console initialized. Type 'help' for a list of commands.");
    }

    private void registerCommands() {
        addCommand(new HelpCommand());
        addCommand(new ClearCommand());
        addCommand(new ScriptCommands.ListScriptsCommand());
        addCommand(new ScriptCommands.EnableScriptCommand());
        addCommand(new ScriptCommands.DisableScriptCommand());
        addCommand(new ScriptCommands.RefreshScriptsCommand());
        addCommand(new ScriptCommands.DisableAllCommand());
    }

    private void addCommand(ConsoleCommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    public void executeCommand(String input) {
        if (input == null || input.trim().isEmpty()) return;

        String trimmedInput = input.trim();
        log(trimmedInput, ConsoleMessage.MessageType.COMMAND);
        addCommandToHistory(trimmedInput);

        String[] parts = trimmedInput.split("\\s+");
        String commandName = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        ConsoleCommand command = commands.get(commandName);
        if (command != null) {
            try {
                command.execute(args);
            } catch (Exception e) {
                logError("Error executing command '" + commandName + "': " + e.getMessage());
            }
        } else {
            logError("Unknown command: '" + commandName + "'. Type 'help' for a list of commands.");
        }
    }

    private void addCommandToHistory(String command) {
        if (commandHistory.isEmpty() || !commandHistory.getLast().equals(command)) {
            commandHistory.add(command);
            if (commandHistory.size() > MAX_HISTORY_SIZE) {
                commandHistory.removeFirst();
            }
        }
    }

    public void log(String message, ConsoleMessage.MessageType type) {
        for (String line : message.split("\\r?\\n")) {
            messages.add(new ConsoleMessage(line, type));
        }
    }

    public void logInfo(String message) {
        log(message, ConsoleMessage.MessageType.INFO);
    }

    public void logError(String message) {
        log(message, ConsoleMessage.MessageType.ERROR);
    }

    public void logSuccess(String message) {
        log(message, ConsoleMessage.MessageType.SUCCESS);
    }

    public void clear() {
        this.messages.clear();
    }

    public List<ConsoleMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public Map<String, ConsoleCommand> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public List<String> getCommandHistory() {
        return Collections.unmodifiableList(commandHistory);
    }
}