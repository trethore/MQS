/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me.console;

import net.me.console.log.ConsoleManagerAppender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConsoleManager {
    private static final PrintStream originalOut = new PrintStream(new FileOutputStream(FileDescriptor.out), true);
    private static final PrintStream originalErr = new PrintStream(new FileOutputStream(FileDescriptor.err), true);
    private static final int MAX_HISTORY_SIZE = 100;
    private final List<ConsoleMessage> messages = new CopyOnWriteArrayList<>();
    private final Map<String, ConsoleCommand> commands = new HashMap<>();
    private final List<String> commandHistory = new ArrayList<>();

    private ConsoleManagerAppender slf4jAppender;

    public void init() {
        logSuccess("Console initialized. Type 'help' for a list of commands.");
    }

    public void addCommand(ConsoleCommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    private List<String> parseArguments(String commandLine) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (char c : commandLine.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
                if (!inQuotes) {
                    flushToken(tokens, sb);
                }
            } else if (c == ' ' && !inQuotes) {
                flushToken(tokens, sb);
            } else {
                sb.append(c);
            }
        }

        flushToken(tokens, sb);
        return tokens;
    }

    private void flushToken(List<String> tokens, StringBuilder sb) {
        if (sb.isEmpty()) {
            return;
        }
        tokens.add(sb.toString());
        sb.setLength(0);
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
        applyLogRedirectState(enable);
    }

    private void applyLogRedirectState(boolean enable) {
        Logger rootLogger = (Logger) LogManager.getRootLogger();

        if (enable) {
            System.setOut(new PrintStream(new ConsoleOutputStream(this, ConsoleMessage.MessageType.INFO, originalOut), true));
            System.setErr(new PrintStream(new ConsoleOutputStream(this, ConsoleMessage.MessageType.ERROR, originalErr), true));

            if (this.slf4jAppender == null) {
                this.slf4jAppender = ConsoleManagerAppender.createAppender(this);
            }
            this.slf4jAppender.start();
            rootLogger.addAppender(this.slf4jAppender);

        } else {
            if (this.slf4jAppender != null) {
                rootLogger.removeAppender(this.slf4jAppender);
                this.slf4jAppender.stop();
            }
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private static class ConsoleOutputStream extends ByteArrayOutputStream {
        private final ConsoleManager consoleManager;
        private final ConsoleMessage.MessageType messageType;
        private final String lineSeparator = System.lineSeparator();
        private final PrintStream originalStream;

        public ConsoleOutputStream(ConsoleManager consoleManager, ConsoleMessage.MessageType messageType, PrintStream originalStream) {
            this.consoleManager = consoleManager;
            this.messageType = messageType;
            this.originalStream = originalStream;
        }

        @Override
        public void flush() throws IOException {
            synchronized (this) {
                super.flush();
                String output = this.toString();
                super.reset();

                if (output.isEmpty() || output.equals(lineSeparator)) {
                    return;
                }

                if (output.endsWith(lineSeparator)) {
                    output = output.substring(0, output.length() - lineSeparator.length());
                }

                consoleManager.log(output, messageType);
                originalStream.println(output);
            }
        }
    }
}
