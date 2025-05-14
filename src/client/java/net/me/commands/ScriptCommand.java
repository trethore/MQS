package net.me.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.fabricmc.loader.api.FabricLoader;
import net.me.Main; // Changed import to Main
import net.me.scripting.ScriptManager; // Your ScriptManager
import net.me.scripting.js.JsInstanceProxy; // Import JsInstanceProxy


public class ScriptCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, ScriptManager scriptManager) {
        dispatcher.register(CommandManager.literal("runscript")
            .requires(source -> source.hasPermissionLevel(2)) // Example permission
            .then(CommandManager.argument("scriptName", StringArgumentType.greedyString())
                .executes(context -> {
                    String scriptName = StringArgumentType.getString(context, "scriptName");
                    Path scriptPath = FabricLoader.getInstance().getConfigDir()
                                        .resolve(Main.MOD_ID) // Changed MOD_ID reference to Main.MOD_ID
                                        .resolve(scriptName + (scriptName.endsWith(".js") ? "" : ".js"));

                    if (!Files.exists(scriptPath)) {
                        context.getSource().sendError(Text.literal("Script not found: " + scriptPath));
                        return 0;
                    }

                    try {
                        String scriptContent = Files.readString(scriptPath);
                        context.getSource().sendFeedback(() -> Text.literal("Executing script: " + scriptName), true);

                        long startTime = System.nanoTime();
                        org.graalvm.polyglot.Value result = scriptManager.executeScript(scriptContent, scriptName);
                        long endTime = System.nanoTime();
                        double durationMs = (endTime - startTime) / 1_000_000.0;

                        String resultString = "null";
                        if (result != null && !result.isNull()) {
                             if (result.isHostObject() && result.asHostObject() instanceof JsInstanceProxy) {
                                resultString = ((JsInstanceProxy)result.asHostObject()).getJavaInstance().toString();
                            } else if (result.isProxyObject()){
                                resultString = result.toString(); // Proxy might have custom toString
                            }
                            else {
                                resultString = result.toString();
                            }
                        }

                        return 1;
                    } catch (IOException e) {
                        context.getSource().sendError(Text.literal("Error reading script: " + e.getMessage()));
                        Main.LOGGER.error("Error reading script {}", scriptName, e); // Changed LOGGER reference to Main.LOGGER
                        return 0;
                    } catch (Exception e) {
                        context.getSource().sendError(Text.literal("Error executing script: " + e.getMessage()));
                        Main.LOGGER.error("Error executing script {}", scriptName, e); // Changed LOGGER reference to Main.LOGGER
                        return 0;
                    }
                })
            )
        );
    }
}
