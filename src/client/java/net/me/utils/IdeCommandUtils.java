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

package net.me.utils;

import net.me.Main;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IdeCommandUtils {
    private static final String DEFAULT_IDE_COMMAND = "code";
    private static final Path DEFAULT_SCRIPTS_DIRECTORY = Main.MOD_DIR.resolve("scripts");
    private static final String WINDOWS_SHELL = "cmd.exe";
    private static final String WINDOWS_SHELL_EXECUTE_FLAG = "/c";

    private IdeCommandUtils() {
    }

    public static String getDefaultIdeCommand(String command) {
        if (command == null || command.isBlank()) {
            return DEFAULT_IDE_COMMAND;
        }

        return command.trim();
    }

    public static Path getDefaultScriptsDirectory() {
        return DEFAULT_SCRIPTS_DIRECTORY.toAbsolutePath().normalize();
    }

    public static Path openPathInIde(String ideCommand, String requestedPath) throws IOException {
        List<String> commandParts = tokenizeCommand(getDefaultIdeCommand(ideCommand));
        if (commandParts.isEmpty()) {
            throw new IllegalArgumentException("IDE command cannot be empty.");
        }

        Path targetPath = resolveTargetPath(requestedPath);
        List<String> processCommand = new ArrayList<>(commandParts.size() + 1);
        processCommand.addAll(commandParts);
        processCommand.add(targetPath.toString());

        startProcess(processCommand);

        return targetPath;
    }

    public static Path pickDirectory(String requestedPath) throws IOException {
        Path defaultDirectory = resolveDialogDirectory(requestedPath);
        String selectedPath = TinyFileDialogs.tinyfd_selectFolderDialog(
                "Select Project Folder",
                defaultDirectory.toString()
        );

        if (selectedPath == null || selectedPath.isBlank()) {
            return null;
        }

        try {
            return Path.of(selectedPath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Invalid path selected.", exception);
        }
    }

    private static void startProcess(List<String> processCommand) throws IOException {
        try {
            createProcessBuilder(processCommand).start();
        } catch (IOException exception) {
            if (!isWindows()) {
                throw exception;
            }

            List<String> shellCommand = new ArrayList<>(processCommand.size() + 2);
            shellCommand.add(WINDOWS_SHELL);
            shellCommand.add(WINDOWS_SHELL_EXECUTE_FLAG);
            shellCommand.addAll(processCommand);
            createProcessBuilder(shellCommand).start();
        }
    }

    private static ProcessBuilder createProcessBuilder(List<String> command) {
        return new ProcessBuilder(command)
                .directory(Main.MOD_DIR.toFile());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static Path resolveDialogDirectory(String requestedPath) throws IOException {
        Path targetPath = resolveTargetPath(requestedPath);
        if (Files.isDirectory(targetPath)) {
            return targetPath;
        }

        Path parent = targetPath.getParent();
        if (parent != null && Files.isDirectory(parent)) {
            return parent;
        }

        Path defaultScriptsDirectory = getDefaultScriptsDirectory();
        Files.createDirectories(defaultScriptsDirectory);
        return defaultScriptsDirectory;
    }

    private static Path resolveTargetPath(String requestedPath) throws IOException {
        String sanitizedPath = requestedPath == null ? "" : requestedPath.trim();
        Path targetPath;

        if (sanitizedPath.isEmpty()) {
            targetPath = getDefaultScriptsDirectory();
            Files.createDirectories(targetPath);
            return targetPath;
        }

        try {
            targetPath = Path.of(PathUtils.expandHomeDirectory(sanitizedPath));
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Invalid path: " + sanitizedPath, exception);
        }

        if (!targetPath.isAbsolute()) {
            targetPath = Main.MOD_DIR.resolve(targetPath);
        }

        return targetPath.toAbsolutePath().normalize();
    }

    private static List<String> tokenizeCommand(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        TokenizerState state = new TokenizerState();

        for (int index = 0; index < command.length(); index += 1) {
            char character = command.charAt(index);
            processCharacter(tokens, currentToken, state, character);
        }

        if (state.hasUnmatchedSyntax()) {
            throw new IllegalArgumentException("Invalid IDE command: unmatched escape or quote.");
        }

        appendToken(tokens, currentToken);
        return tokens;
    }

    private static void appendToken(List<String> tokens, StringBuilder currentToken) {
        if (currentToken.isEmpty()) {
            return;
        }

        tokens.add(currentToken.toString());
        currentToken.setLength(0);
    }

    private static void processCharacter(List<String> tokens, StringBuilder currentToken, TokenizerState state, char character) {
        if (state.escaping) {
            currentToken.append(character);
            state.escaping = false;
        } else if (shouldStartEscape(state, character)) {
            state.escaping = true;
        } else if (character == '\'' && !state.inDoubleQuotes) {
            state.inSingleQuotes = !state.inSingleQuotes;
        } else if (character == '"' && !state.inSingleQuotes) {
            state.inDoubleQuotes = !state.inDoubleQuotes;
        } else if (Character.isWhitespace(character) && !state.isInsideQuotes()) {
            appendToken(tokens, currentToken);
        } else {
            currentToken.append(character);
        }
    }

    private static boolean shouldStartEscape(TokenizerState state, char character) {
        return character == '\\' && state.inDoubleQuotes;
    }

    private static final class TokenizerState {
        private boolean inSingleQuotes;
        private boolean inDoubleQuotes;
        private boolean escaping;

        private boolean hasUnmatchedSyntax() {
            return this.escaping || this.inSingleQuotes || this.inDoubleQuotes;
        }

        private boolean isInsideQuotes() {
            return this.inSingleQuotes || this.inDoubleQuotes;
        }
    }
}
