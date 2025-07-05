package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.console.ConsoleMessage;
import net.minecraft.client.MinecraftClient;

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
            } catch (NumberFormatException e) {
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

        MinecraftClient.getInstance().keyboard.setClipboard(textToCopy);
        cm.logSuccess("Copied " + tail.size() + " lines to clipboard.");
    }
}