package net.me.scripting;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class ScriptManagerTest {

    private ScriptManager scriptManager;
    private static Path tempModDir;

    @TempDir
    static Path staticTempDir; // Used to assign to tempModDir in a static context

    @BeforeAll
    static void setUpClass() throws Exception {
        tempModDir = staticTempDir;

        // Use reflection to set Main.MOD_DIR
        // This is necessary because Main.MOD_DIR is final and initialized
        // using FabricLoader, which is not available in unit tests.
        Field modDirField = Main.class.getDeclaredField("MOD_DIR");
        modDirField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(modDirField, modDirField.getModifiers() & ~Modifier.FINAL);

        modDirField.set(null, tempModDir);

        // Create dummy mapping files that MappingsManager might try to load
        // to prevent FileNotFoundExceptions if MappingsManager.init() is called.
        Path mappingsDir = tempModDir.resolve("mappings");
        Files.createDirectories(mappingsDir);
        Files.createFile(mappingsDir.resolve("mappings_yarn_to_official.csv"));
        Files.createFile(mappingsDir.resolve("mappings_yarn_to_intermediary.csv"));
        Files.createFile(mappingsDir.resolve("fields_yarn_to_intermediary.csv"));
        Files.createFile(mappingsDir.resolve("methods_yarn_to_intermediary.csv"));
        // Add empty lines to simulate CSV structure if needed, though empty files might suffice
        // for MappingsManager to not throw an immediate error.
    }

    @BeforeEach
    void setUp() {
        // Reset ScriptManager instance before each test to ensure isolation
        // This is tricky with singletons. A better way would be a reset method or making it non-singleton for tests.
        // For now, we'll rely on cleaning up its state.
        scriptManager = ScriptManager.getInstance();
        // Clear any scripts from previous tests
        scriptManager.getAllScripts().forEach(script -> scriptManager.removeScript(script.getName()));
        
        // Ensure ScriptManager's internal context is initialized, which also loads mappings
        // This might create the "scripts" directory inside tempModDir
        try {
            scriptManager.init(); 
        } catch (Exception e) {
            // If MappingsManager still fails, some tests might be compromised.
            System.err.println("Warning: ScriptManager.init() failed in test setup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @AfterEach
    void tearDown() {
        // Clean up scripts from the scriptManager to ensure test isolation
        scriptManager.getAllScripts().forEach(s -> scriptManager.removeScript(s.getName()));
    }


    @Test
    void testAddAndGetScript() {
        Path dummyPath = Paths.get("dummy.js");
        Script testScript = new Script("testScript", dummyPath);
        scriptManager.addScript(testScript);

        Script retrievedScript = scriptManager.getScript("testScript");
        assertNotNull(retrievedScript, "Retrieved script should not be null");
        assertEquals("testScript", retrievedScript.getName(), "Script name should match");
        assertSame(testScript, retrievedScript, "Retrieved script should be the same instance");

        Collection<Script> allScripts = scriptManager.getAllScripts();
        assertNotNull(allScripts, "Collection of all scripts should not be null");
        assertEquals(1, allScripts.size(), "There should be one script in the manager");
        assertTrue(allScripts.contains(testScript), "All scripts should contain the added script");
    }

    @Test
    void testRemoveScript() {
        Path dummyPath = Paths.get("dummyRemove.js");
        Script scriptToRemove = new Script("scriptToRemove", dummyPath);
        scriptManager.addScript(scriptToRemove);

        // Ensure it's added
        assertNotNull(scriptManager.getScript("scriptToRemove"), "Script should be present before removal");

        scriptManager.removeScript("scriptToRemove");
        assertNull(scriptManager.getScript("scriptToRemove"), "Script should be null after removal");

        Collection<Script> allScripts = scriptManager.getAllScripts();
        assertTrue(allScripts.isEmpty(), "All scripts collection should be empty after removal");
    }

    @Test
    void testGetAllScripts() {
        Script script1 = new Script("script1", Paths.get("s1.js"));
        Script script2 = new Script("script2", Paths.get("s2.js"));
        Script script3 = new Script("script3", Paths.get("s3.js"));

        scriptManager.addScript(script1);
        scriptManager.addScript(script2);
        scriptManager.addScript(script3);

        Collection<Script> allScripts = scriptManager.getAllScripts();
        assertNotNull(allScripts, "Collection of scripts should not be null");
        assertEquals(3, allScripts.size(), "Should be 3 scripts in the manager");
        assertTrue(allScripts.contains(script1), "Should contain script1");
        assertTrue(allScripts.contains(script2), "Should contain script2");
        assertTrue(allScripts.contains(script3), "Should contain script3");
    }

    @Test
    void testCreateDefaultScriptContext() {
        // ScriptManager.init() called in @BeforeEach should have initialized mappings.
        // If MappingsManager is too complex to initialize correctly, this test might be limited.
        Context ctx = null;
        try {
            ctx = scriptManager.createDefaultScriptContext();
            assertNotNull(ctx, "Created context should not be null");

            // Try a simple evaluation that relies on one of the bound functions.
            // This implicitly tests if the context is configured to some extent.
            Value result = ctx.eval("js", "typeof importClass");
            assertEquals("function", result.asString(), "'typeof importClass' should return 'function'");

        } catch (Exception e) {
            // If MappingsManager.init() fails, createDefaultScriptContext might fail.
            fail("testCreateDefaultScriptContext failed due to an exception during context creation or evaluation: " + e.getMessage(), e);
        } finally {
            if (ctx != null) {
                ctx.close(true); // Close context to free resources
            }
        }
    }
}
