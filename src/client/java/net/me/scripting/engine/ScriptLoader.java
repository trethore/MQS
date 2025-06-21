package net.me.scripting.engine;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptLoader {

    public ScriptLoader() {
    }

    public Map<String, Value> loadModules(Path scriptPath, Context context, ThreadLocal<Map<String, Value>> perFileExports) {
        perFileExports.set(new HashMap<>());
        try {
            Source source = Source.newBuilder("js", scriptPath.toFile())
                    .mimeType("application/javascript+module")
                    .build();
            context.eval(source);

            return perFileExports.get();
        } catch (Exception e) {
            Main.LOGGER.error("Failed to load or parse script file for modules: {}", scriptPath, e);
            return Collections.emptyMap();
        } finally {
            perFileExports.remove();
        }
    }
}