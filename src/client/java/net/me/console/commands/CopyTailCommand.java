/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.console.ConsoleMessage;
import net.me.utils.McUtils;

import java.util.List;
import java.util.stream.Collectors;

public class CopyTailCommand extends ConsoleCommand {

    public CopyTailCommand(ConsoleManager consoleManager) {
        super(consoleManager, "copytail", "Copies the last <n> lines of the console to the clipboard.", "copytail <number=10>");
    }

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = this.getConsoleManager();
        if (args.length > 1) {
            cm.logError("Invalid arguments. Usage: " + getUsage());
            return;
        }

        int numberOfLines;
        if (args.length == 0) {
            numberOfLines = 10;
        } else {
            try {
                numberOfLines = Integer.parseInt(args[0]);
            } catch (NumberFormatException _) {
                cm.logError("Invalid argument '" + args[0] + "'. Must be a number.");
                return;
            }
        }

        if (numberOfLines <= 0) {
            cm.logError("Number of lines must be positive.");
            return;
        }


        List<ConsoleMessage> allMessages = cm.getMessages();

        if (allMessages.size() <= 1) {
            cm.logInfo("No previous messages to copy.");
            return;
        }

        List<ConsoleMessage> messagesToCopy = allMessages.subList(0, allMessages.size() - 1);

        int startIndex = Math.max(0, messagesToCopy.size() - numberOfLines);

        List<ConsoleMessage> tail = messagesToCopy.subList(startIndex, messagesToCopy.size());


        if (tail.isEmpty()) {
            cm.logInfo("Console is empty, nothing to copy.");
            return;
        }

        String textToCopy = tail.stream()
                .map(msg -> String.format("[%s] %s", msg.timestamp(), msg.text()))
                .collect(Collectors.joining(System.lineSeparator()));

        McUtils.getMc().keyboardHandler.setClipboard(textToCopy);
        cm.logSuccess("Copied " + tail.size() + " lines to clipboard.");
    }
}
