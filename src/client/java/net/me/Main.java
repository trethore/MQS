package net.me;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.me.command.CommandManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.ScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "my-qol-scripts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MC_VERSION = "1.21.4";
    public static final Path MOD_DIR = FabricLoader.getInstance().getGameDir().resolve(MOD_ID);

    @Override
    public void onInitializeClient() {
        MappingsManager.getInstance().init();
        ScriptManager.getInstance().init();
        CommandManager.getInstance().init();
        LOGGER.info("Hello from MyQOLScripts!");
    }
}