package net.me;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.me.commands.TestJsCommand;
import net.me.mappings.MappingsManager;
import net.me.scripting.ScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "my-qol-scripts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String MC_VERSION = "1.21.4";


    @Override
    public void onInitializeClient() {
        LOGGER.info("Hello from MyQOLScripts!");
        MappingsManager.getInstance().init();
        ScriptManager.getInstance().init();

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> TestJsCommand.register(dispatcher)
        );
        LOGGER.info("Registered /testjs command.");
    }


}