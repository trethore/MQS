package net.me.console.commands;

import net.me.console.ConsoleCommand;
import net.me.console.ConsoleManager;
import net.me.console.ConsoleMessage;
import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.stream.Collectors;

public class CopyTailCommand implements ConsoleCommand {

    @Override
    public void execute(String[] args) {
        ConsoleManager cm = ConsoleManager.getInstance();

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

        List<ConsoleMessage> messages = cm.getMessages();
        if (messages.isEmpty()) {
            cm.logInfo("Console is empty, nothing to copy.");
            return;
        }
        int lastMessageIndex = messages.size() - 1;
        int startIndex = Math.max(0, messages.size() - numberOfLines - 1);
        List<ConsoleMessage> tail = messages.subList(startIndex,lastMessageIndex);

        String textToCopy = tail.stream()
                .map(msg -> String.format("[%s] %s", msg.timestamp(), msg.text()))
                .collect(Collectors.joining(System.lineSeparator()));

        MinecraftClient.getInstance().keyboard.setClipboard(textToCopy);
        cm.logSuccess("Copied " + tail.size() + " lines to clipboard.");
    }

    @Override
    public String getName() {
        return "copytail";
    }

    @Override
    public String getDescription() {
        return "Copies the last <n> lines of the console to the clipboard.";
    }

    @Override
    public String getUsage() {
        return "copytail <number=10>";
    }
}