package net.me.utils;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@SuppressWarnings("unused")
public final class ChatUtils {
    public final static String TAG = Formatting.GRAY + "[" + Formatting.GREEN + "MQS" + Formatting.GRAY + "] " + Formatting.RESET;

    private ChatUtils() {
    }

    public static void sendChatMessage(String s) {
        McUtils.getPlayer().ifPresent(player ->
                player.networkHandler.sendChatMessage(s)
        );
    }

    public static void sendChatCommand(String s) {
        McUtils.getPlayer().ifPresent(player ->
                player.networkHandler.sendChatCommand(s)
        );
    }

    public static void addInfoChatMessage(String message, boolean prefix) {
        addChatMessage(message, Level.INFO, prefix);
    }

    public static void addWarnChatMessage(String message, boolean prefix) {
        addChatMessage(message, Level.WARN, prefix);
    }

    public static void addErrorChatMessage(String message, boolean prefix) {
        addChatMessage(message, Level.ERROR, prefix);
    }

    public static void addSuccessChatMessage(String message, boolean prefix) {
        addChatMessage(message, Level.SUCCESS, prefix);
    }

    public static void addChatMessage(String message, Level level, boolean prefix) {
        Text text = Text.literal(message).formatted(level.getFormatting());
        if (prefix) {
            text = Text.literal(TAG).append(text);
        }
        Text finalText = text;
        McUtils.getPlayer().ifPresent(p -> p.sendMessage(finalText, false));
    }

    public static void addRawMessage(String message) {
        McUtils.getPlayer().ifPresent(player -> player.sendMessage(Text.literal(message), false));
    }


    public enum Level {
        ERROR(Formatting.RED),
        INFO(Formatting.WHITE),
        WARN(Formatting.GOLD),
        SUCCESS(Formatting.GREEN);
        private final Formatting fmt;

        Level(Formatting fmt) {
            this.fmt = fmt;
        }

        public Formatting getFormatting() {
            return fmt;
        }
    }


}
