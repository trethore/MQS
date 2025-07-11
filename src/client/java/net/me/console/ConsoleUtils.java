package net.me.console;

import java.util.Optional;

public class ConsoleUtils {

    public static final String TRUE_STRING = "true";
    public static final String FALSE_STRING = "false";

    public static Optional<Boolean> parseBooleanArg(String arg) {
        if (TRUE_STRING.equalsIgnoreCase(arg)) return Optional.of(true);
        if (FALSE_STRING.equalsIgnoreCase(arg)) return Optional.of(false);
        return Optional.empty();
    }
}
