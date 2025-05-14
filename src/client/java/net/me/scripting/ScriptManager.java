package net.me.scripting;

import net.fabricmc.loader.impl.lib.mappingio.MappingReader;
import net.fabricmc.loader.impl.lib.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;
import net.me.scripting.js.*;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ScriptManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptManager.class);
    private final Context jsContext;
    private final MappingData mappingData;

    public ScriptManager(Path mappingFilePath) {
        MappingTree mappingTree = new MemoryMappingTree();
        boolean parsed = false;
        if (mappingFilePath != null && Files.exists(mappingFilePath)) {
            try (InputStream is = Files.newInputStream(mappingFilePath)) {
                Tiny2FileReader.read(actualFileReader, mappingTree);
                parsed = true;
            } catch (IOException e) {
                LOGGER.error("Failed to parse mapping file: {}", mappingFilePath, e);
            }
        } else {
            LOGGER.warn("No mapping file provided or found. Named access will be limited.");
        }

        if (parsed) {
            this.mappingData = new MappingData(mappingTree, "named", "official");
        } else {
            this.mappingData = null; // Or a dummy MappingData
        }


        this.jsContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true) // Allow any, we filter via proxies
                .allowExperimentalOptions(true)
                .option("js.nashorn-compat", "true")
                .option("js.foreign-object-prototype", "true") // Important for proxy interaction
                .option("engine.WarnInterpreterOnly", "false") // Suppress GraalVM warnings if not using Native Image
                .build();

        try {
            jsContext.eval(Source.newBuilder("js", "load('nashorn:mozilla_compat.js');", "NashornCompat").build());
        } catch (IOException e) {
            LOGGER.error("Failed to load Nashorn compatibility", e);
        }

        Value bindings = jsContext.getBindings("js");

        if (this.mappingData != null) {
            // Expose root packages (e.g., "net", "com")
            // Collect unique root package names from your mappings
            Set<String> rootPackageNames = new HashSet<>();
            mappingData.getMappingTree().getClasses().stream()
                .map(cls -> cls.getName(mappingData.getNamedNsId()))
                .filter(Objects::nonNull)
                .forEach(className -> {
                    int firstSlash = className.indexOf('/');
                    if (firstSlash > 0) {
                        rootPackageNames.add(className.substring(0, firstSlash));
                    }
                });

            for (String rootPkgName : rootPackageNames) {
                 // For "net.minecraft...", put "net" as the root. The JsNamespace will handle sub-packages.
                 // The prefix for JsNamespace should end with a dot.
                 bindings.putMember(rootPkgName, new JsNamespace(rootPkgName + ".", mappingData, this));
            }
        } else {
            LOGGER.warn("Mappings not loaded. JavaScript will have limited named access.");
            // You could still expose Java.type here, or a limited set of known official classes
            bindings.putMember("Java", jsContext.getPolyglotBindings().getMember("Java"));
        }

        bindings.putMember("console", new JsConsole());
    }

    public MappingData getMappingParser() { // Renamed to getMappingData
        return mappingData;
    }

    public Context getJsContext() {
        return jsContext;
    }

    public Value executeScript(String scriptContent, String scriptName) {
        try {
            Source source = Source.newBuilder("js", scriptContent, scriptName).build();
            return jsContext.eval(source);
        } catch (Exception e) {
            LOGGER.error("Error executing script '{}':", scriptName, e);
            // Optionally, rethrow or return an error Value
            throw new RuntimeException("Script execution failed: " + scriptName, e);
        }
    }

    // Helper to quickly execute simple commands for testing
    public Value eval(String command) {
        return jsContext.eval("js", command);
    }

    public Logger getLogger() { return LOGGER; }
}
