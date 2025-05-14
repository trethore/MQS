package net.me.scripting.js;

import org.graalvm.polyglot.HostAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsConsole {
    private static final Logger SCRIPT_CONSOLE_LOGGER = LoggerFactory.getLogger("ScriptingConsole");

    @HostAccess.Export
    public void log(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] == null ? "null" : args[i].toString());
            if (i < args.length - 1) {
                sb.append(" ");
            }
        }
        SCRIPT_CONSOLE_LOGGER.info("[JS] " + sb.toString());
    }

    @HostAccess.Export
    public void error(Object... args) {
         StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i] == null ? "null" : args[i].toString());
            if (i < args.length - 1) {
                sb.append(" ");
            }
        }
        SCRIPT_CONSOLE_LOGGER.error("[JS] " + sb.toString());
    }
}