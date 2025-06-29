package net.me.console.log;

import net.me.console.ConsoleManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

@Plugin(name = "ConsoleManagerAppender", category = "Core", elementType = "appender", printObject = true)
public class ConsoleManagerAppender extends AbstractAppender {
    private final ConsoleManager consoleManager;

    private ConsoleManagerAppender(ConsoleManager consoleManager) {
        super("ConsoleManagerAppender", null, PatternLayout.createDefaultLayout(), true, null);
        this.consoleManager = consoleManager;
    }

    @PluginFactory
    public static ConsoleManagerAppender createAppender(ConsoleManager consoleManager) {
        return new ConsoleManagerAppender(consoleManager);
    }

    @Override
    public void append(LogEvent event) {
        String message = String.format("[%s/%s] %s",
                event.getThreadName(),
                event.getLoggerName().substring(event.getLoggerName().lastIndexOf('.') + 1),
                event.getMessage().getFormattedMessage()
        );

        Throwable thrown = event.getThrown();
        if (thrown != null) {
            message += ": " + thrown.getMessage();
        }

        if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
            consoleManager.logError(message);
            if (thrown != null) {
                for (StackTraceElement ste : thrown.getStackTrace()) {
                    consoleManager.logError("  at " + ste.toString());
                }
            }
        } else {
            consoleManager.logInfo(message);
        }
    }
}