package net.me.console.log;

import net.me.console.ConsoleManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.Level;

@Plugin(name = "ConsoleManagerAppender", category = "Core", elementType = "appender", printObject = true)
public class ConsoleManagerAppender extends AbstractAppender {

    private ConsoleManagerAppender() {
        super("ConsoleManagerAppender", null, PatternLayout.createDefaultLayout(), true, null);
    }

    @PluginFactory
    public static ConsoleManagerAppender createAppender() {
        return new ConsoleManagerAppender();
    }

    @Override
    public void append(LogEvent event) {
        ConsoleManager cm = ConsoleManager.getInstance();
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
            cm.logError(message);
            if (thrown != null) {
                for (StackTraceElement ste : thrown.getStackTrace()) {
                    cm.logError("  at " + ste.toString());
                }
            }
        } else {
            cm.logInfo(message);
        }
    }
}