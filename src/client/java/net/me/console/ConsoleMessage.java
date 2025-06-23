package net.me.console;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record ConsoleMessage(String text, MessageType type, String timestamp) {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ConsoleMessage(String text, MessageType type) {
        this(text, type, LocalTime.now().format(TIME_FORMATTER));
    }

    public enum MessageType {
        INFO(0xFFFFFFFF),
        ERROR(0xFFF38BA8),
        COMMAND(0xFF89B4FA),
        SUCCESS(0xFFA6E3A1);

        private final int color;

        MessageType(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }
}