package net.me;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.me.mappings.MappingsManager;
import net.me.scripting.ScriptManager;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "my-qol-scripts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MC_VERSION = "1.21.4";


    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello from MyQOLScripts!");
        MappingsManager.getInstance().init();
        ScriptManager.getInstance().init();
        try {
            Path path2script = FabricLoader.getInstance().getGameDir().resolve(Main.MOD_ID).resolve("scripts");
            path2script = path2script.resolve("test.js");
            String js  = Files.readString(path2script);
            ScriptManager.getInstance().run(js);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}