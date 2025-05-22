package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptTest {

    private static Path tempModDir;

    @TempDir
    static Path staticTempDir; // Used to assign to tempModDir in a static context

    @BeforeAll
    static void setUpClass() throws Exception {
        tempModDir = staticTempDir;

        // Use reflection to set Main.MOD_DIR
        Field modDirField = Main.class.getDeclaredField("MOD_DIR");
        modDirField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(modDirField, modDirField.getModifiers() & ~Modifier.FINAL);

        modDirField.set(null, tempModDir);
        
        // Create dummy mapping files that MappingsManager might try to load
        // This is needed because Script -> createDefaultScriptContext -> ensureContextInitialized -> loadMappings
        Path mappingsDir = tempModDir.resolve("mappings");
        Files.createDirectories(mappingsDir);
        Files.createFile(mappingsDir.resolve("mappings_yarn_to_official.csv"));
        Files.createFile(mappingsDir.resolve("mappings_yarn_to_intermediary.csv"));
        Files.createFile(mappingsDir.resolve("fields_yarn_to_intermediary.csv"));
        Files.createFile(mappingsDir.resolve("methods_yarn_to_intermediary.csv"));

        // Initialize ScriptManager to load mappings, otherwise createDefaultScriptContext will fail.
        // This also creates the "scripts" directory inside tempModDir.
        try {
            ScriptManager.getInstance().init();
        } catch (Exception e) {
            System.err.println("Warning: ScriptManager.init() failed in ScriptTest setup: " + e.getMessage());
            // Depending on the failure, tests below might still fail.
        }
    }

    @Test
    void testScriptContextInitialization() throws IOException {
        Path dummyJsPath = tempModDir.resolve("testScript.js");
        Files.createFile(dummyJsPath); // Create an actual empty file

        Script script = null;
        Context ctx = null;
        try {
            script = new Script("myTestScript", dummyJsPath);
            assertNotNull(script, "Script instance should not be null");

            ctx = script.getDefaultContext();
            assertNotNull(ctx, "Default context from script should not be null");

            Context ctx2 = script.getDefaultContext();
            assertSame(ctx, ctx2, "Repeated calls to getDefaultContext should return the same instance");

            // Check if context is usable (was configured by ScriptManager)
            assertEquals("function", ctx.eval("js", "typeof importClass").asString(), "Context should have importClass bound");

        } catch (Exception e) {
            fail("testScriptContextInitialization failed due to an exception: " + e.getMessage(), e);
        } finally {
            if (ctx != null) {
                ctx.close(true);
            }
            if (Files.exists(dummyJsPath)) {
                Files.delete(dummyJsPath);
            }
        }
    }

    @Test
    void testScriptNameAndPath() {
        String scriptName = "coolScript";
        Path scriptPath = Paths.get("path/to/coolScript.js"); // Does not need to exist

        Script script = new Script(scriptName, scriptPath);

        assertEquals(scriptName, script.getName(), "script.getName() should return the constructor-provided name.");
        assertEquals(scriptPath, script.getScriptPath(), "script.getScriptPath() should return the constructor-provided path.");
    }

    @Test
    void testScriptRunWithDummyFile() throws IOException {
        Path dummyJsPath = tempModDir.resolve("runTestScript.js");
        Files.writeString(dummyJsPath, "console.log('Hello from script'); var a = 1+1; a;"); // Simple JS

        Script script = new Script("runTest", dummyJsPath);
        Context ctx = null;
        try {
            script.run(); // Executes the script file
            
            // Check console output from the script
            List<String> consoleOutput = script.getConsole();
            assertFalse(consoleOutput.isEmpty(), "Console output should not be empty after running script.");
            assertEquals("[runTest] Running script: runTest", consoleOutput.get(0));
            // The actual result of `a;` is implementation-dependent in GraalVM if not explicitly returned.
            // The 'Finished Running the Script' message includes the Value.toString() of the last expression.
            assertTrue(consoleOutput.get(consoleOutput.size()-1).startsWith("[runTest] Finished Running the Script : 2"), 
                "Script should finish and log its result. Last output: " + consoleOutput.get(consoleOutput.size()-1));

        } catch (Exception e) {
            fail("testScriptRunWithDummyFile failed due to an exception: " + e.getMessage(), e);
        } finally {
            ctx = script.getDefaultContext(); // Get context for closing
            if (ctx != null && ctx.getEngine().isActive()) { // Check if engine is active before closing
                 ctx.close(true);
            }
            if (Files.exists(dummyJsPath)) {
                Files.delete(dummyJsPath);
            }
        }
    }
}
