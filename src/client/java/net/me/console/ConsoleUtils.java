package net.me.console;

import java.util.Optional;

public class ConsoleUtils {
    public static Optional<Boolean> parseBooleanArg(String arg) {
        if ("true".equalsIgnoreCase(arg)) return Optional.of(true);
        if ("false".equalsIgnoreCase(arg)) return Optional.of(false);
        return Optional.empty();
    }
}
