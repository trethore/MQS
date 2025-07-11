package net.me.scripting.engine;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScriptContextManager {

    private final Queue<Context> contextPool = new ConcurrentLinkedQueue<>();
    private final ScriptContextFactory contextFactory;
    private final ThreadLocal<Map<String, Value>> perFileExports;

    public ScriptContextManager(ScriptContextFactory contextFactory, ThreadLocal<Map<String, Value>> perFileExports) {
        this.contextFactory = contextFactory;
        this.perFileExports = perFileExports;
        prewarmContextPool();
    }

    public Context getContext() {
        Context context = contextPool.poll();
        if (context == null) {
            Main.LOGGER.warn("Script context pool is empty. Creating a new on-demand context.");
            return contextFactory.createContext(perFileExports);
        }
        return context;
    }

    public void returnContext(Context context) {
        if (context != null) {
            contextFactory.resetContext(context);
            contextPool.offer(context);
        }
    }

    private void prewarmContextPool() {
        Main.LOGGER.info("Pre-warming script context pool...");
        Context context = contextFactory.createContext(perFileExports);
        if (context != null) {
            contextPool.offer(context);
            Main.LOGGER.info("Context pool pre-warmed successfully.");
        } else {
            Main.LOGGER.error("Failed to create a context for the pre-warming pool.");
        }
    }
}