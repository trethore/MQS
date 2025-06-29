package net.me.console;

import net.me.console.commands.*;
import net.me.console.log.ConsoleManagerAppender;
import net.me.scripting.ScriptingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConsoleManager {
    private final List<ConsoleMessage> messages = new CopyOnWriteArrayList<>();
    private final Map<String, ConsoleCommand> commands = new HashMap<>();
    private final List<String> commandHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 100;

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private boolean logsRedirected = false;
    private ConsoleManagerAppender slf4jAppender;
    private ScriptingService scriptingService;

    public void init(ScriptingService scriptingService) {
        this.scriptingService = scriptingService;
        registerCommands();
        logSuccess("Console initialized. Type 'help' for a list of commands.");
    }

    private void registerCommands() {
        addCommand(new HelpCommand(this));
        addCommand(new ClearCommand(this));
        addCommand(new ScriptCommands.ListScriptsCommand(this, scriptingService));
        addCommand(new ScriptCommands.EnableScriptCommand(this, scriptingService));
        addCommand(new ScriptCommands.DisableScriptCommand(this, scriptingService));
        addCommand(new ScriptCommands.RefreshScriptsCommand(this, scriptingService));
        addCommand(new ScriptCommands.RefreshAndReenableCommand(this, scriptingService));
        addCommand(new ScriptCommands.DisableAllCommand(this, scriptingService));
        addCommand(new LogRedirectCommand(this));
        addCommand(new CopyTailCommand(this));
        addCommand(new SaveConfigCommand(this, scriptingService));
        addCommand(new SaveAllConfigsCommand(this, scriptingService));
    }

    private void addCommand(ConsoleCommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    private List<String> parseArguments(String commandLine) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : commandLine.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
                if (!inQuotes && !sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else if (c == ' ' && !inQuotes) {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (!sb.isEmpty()) {
            tokens.add(sb.toString());
        }
        return tokens;
    }

    public void executeCommand(String input) {
        if (input == null || input.trim().isEmpty()) return;

        String trimmedInput = input.trim();
        log(trimmedInput, ConsoleMessage.MessageType.COMMAND);
        addCommandToHistory(trimmedInput);

        List<String> parts = parseArguments(trimmedInput);
        if (parts.isEmpty()) {
            return;
        }

        String commandName = parts.getFirst().toLowerCase();
        String[] args = parts.subList(1, parts.size()).toArray(new String[0]);

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
        if (commandHistory.isEmpty() || !command.equals(commandHistory.getLast())) {
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

    public void setLogRedirect(boolean enable) {
        if (enable == this.logsRedirected) {
            logInfo("Log redirection is already " + (enable ? "enabled." : "disabled."));
            return;
        }

        this.logsRedirected = enable;
        Logger rootLogger = (Logger) LogManager.getRootLogger();

        if (enable) {
            System.setOut(new PrintStream(new ConsoleOutputStream(this, ConsoleMessage.MessageType.INFO), true));
            System.setErr(new PrintStream(new ConsoleOutputStream(this, ConsoleMessage.MessageType.ERROR), true));

            if (this.slf4jAppender == null) {
                this.slf4jAppender = ConsoleManagerAppender.createAppender(this);
            }
            this.slf4jAppender.start();
            rootLogger.addAppender(this.slf4jAppender);

        } else {
            System.setOut(originalOut);
            System.setErr(originalErr);

            if (this.slf4jAppender != null) {
                rootLogger.removeAppender(this.slf4jAppender);
                this.slf4jAppender.stop();
            }
        }
    }

    private static class ConsoleOutputStream extends ByteArrayOutputStream {
        private final ConsoleManager consoleManager;
        private final ConsoleMessage.MessageType messageType;
        private final String lineSeparator = System.lineSeparator();

        public ConsoleOutputStream(ConsoleManager consoleManager, ConsoleMessage.MessageType messageType) {
            this.consoleManager = consoleManager;
            this.messageType = messageType;
        }

        @Override
        public void flush() throws IOException {
            synchronized (this) {
                super.flush();
                String record = this.toString();
                super.reset();

                if (record.isEmpty() || record.equals(lineSeparator)) {
                    return;
                }

                if (record.endsWith(lineSeparator)) {
                    record = record.substring(0, record.length() - lineSeparator.length());
                }

                consoleManager.log(record, messageType);
            }
        }
    }
}