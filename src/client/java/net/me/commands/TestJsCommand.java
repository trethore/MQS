package net.me.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.Main;
import net.me.scripting.Script;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;


public class TestJsCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("testjs")
                .executes(TestJsCommand::runTestScript));
    }

    private static int runTestScript(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        source.sendFeedback(Text.literal("Executing test.js script..."));
        Main.LOGGER.info("'/testjs' command received. Executing test.js...");

        try {
            Path path2script = Main.MOD_DIR.resolve("scripts").resolve("test.js");

            if (!Files.exists(path2script)) {
                String errorMessage = "test.js not found at: " + path2script;
                Main.LOGGER.error(errorMessage);
                source.sendError(Text.literal(errorMessage));
                return 0;
            }

            Script jsScript = new Script("test", path2script);

            jsScript.run();

            String successMessage = "test.js script executed successfully.";
            Main.LOGGER.info(successMessage);
            source.sendFeedback(Text.literal(successMessage));
            return 1;
        } catch (Exception e) {
            String errorMessage = "Error executing script: " + e.getMessage();
            Main.LOGGER.error(errorMessage, e);
            source.sendError(Text.literal("Error executing script. Check console for details."));
        }
        return 0;
    }
}
