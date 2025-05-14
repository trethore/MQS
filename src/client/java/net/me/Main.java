package net.me; // Your actual package

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.me.scripting.ScriptManager; // Your ScriptManager
import net.me.commands.ScriptCommand; // Your command class

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Main implements ModInitializer { // Kept class name as Main
    public static final String MOD_ID = "myqolscripts"; // Change if needed
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String MC_VERSION = "1.21.4";
    private static ScriptManager SCRIPT_MANAGER;

    @Override
    public void onInitialize() {
        LOGGER.info("My QOL Scripts Initializing...");

        Path configDir = FabricLoader.getInstance().getGameDir().resolve(MOD_ID).resolve("mappings");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory: {}", configDir, e);
        }

        Path mappingFileInConfig = configDir.resolve("yarn1.21.4.tiny");

        // Copy mappings from resources to config if not present
        if (!Files.exists(mappingFileInConfig)) {
            try (InputStream internalMappingStream = Main.class.getResourceAsStream("/assets/" + MOD_ID + "/mappings/yarn-mappings.tiny")) { // Changed class reference to Main
                if (internalMappingStream == null) {
                    LOGGER.error("FATAL: Packaged yarn-mappings.tiny not found in resources!");
                    // Handle this critical error, perhaps by disabling the mod's scripting features
                } else {
                    Files.copy(internalMappingStream, mappingFileInConfig, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Copied yarn-mappings.tiny to config directory.");
                }
            } catch (IOException e) {
                LOGGER.error("Failed to copy yarn-mappings.tiny to config directory", e);
            }
        }

        if (!Files.exists(mappingFileInConfig)) {
             LOGGER.error("CRITICAL: Mapping file not found at {}. Scripts will not have named access.", mappingFileInConfig);
            SCRIPT_MANAGER = new ScriptManager(null);
        } else {
            SCRIPT_MANAGER = new ScriptManager(mappingFileInConfig);
        }


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            ScriptCommand.register(dispatcher, SCRIPT_MANAGER) // Assuming you have a ScriptCommand class
        );

        // Test Script Execution
        Path testScriptPath = configDir.resolve("test.js");
        if (!Files.exists(testScriptPath)) {
            try {
                Files.writeString(testScriptPath,
                    "// My QOL Scripts - Test Script\n" +
                    "const BlockPos = net.minecraft.util.math.BlockPos;\n" +
                    "let pos = BlockPos.new(10, 64, 20);\n" +
                    "console.log('Created BlockPos at: ' + pos.getX() + ', ' + pos.getY() + ', ' + pos.getZ());\n\n" +
                    "const MathConstants = net.minecraft.util.math.MathConstants;\n"+
                    "// Accessing static field (note: requires explicit handling in JsClassProxy for direct .FIELD_NAME)\n" +
                    "// For now, if not explicitly handled, you might need to access it via a method if one exists\n" +
                    "// or use Java.type as a fallback if the proxy doesn't find it.\n" +
                    "// let pi = MathConstants.PI; // This would need specific JsClassProxy handling for static fields\n"+
                    "// console.log('PI: ' + pi);\n" +
                    "console.log('To access static fields directly like MathConstants.PI, JsClassProxy needs to implement it.');\n" +
                    "console.log('Accessing Minecraft version through Java.type as an example:');\n" +
                    "const MinecraftVersion = Java.type('net.minecraft.MinecraftVersion');\n" +
                    "console.log('MC Version: ' + MinecraftVersion.CURRENT.getName());"

                );
            } catch (IOException e) {
                LOGGER.error("Could not create sample test.js", e);
            }
        }
        try {
            String scriptContent = Files.readString(testScriptPath);
            SCRIPT_MANAGER.executeScript(scriptContent, "test.js");
        } catch (Exception e) {
            LOGGER.error("Error running test.js", e);
        }
    }
}
