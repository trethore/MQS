/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.me.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.Main;
import net.me.command.Command;
import net.me.command.CommandManager;
import net.me.utils.ChatUtils;
import net.me.utils.PathUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.regex.Pattern;

public class TemplateCommand extends Command {
    private static final String ID_ARG = "id";
    private static final String NAME_ARG = "name";
    private static final String VERSION_ARG = "version";
    private static final String PATH_ARG = "path";
    private static final Pattern VALID_SCRIPT_ID_PATTERN = Pattern.compile("^[A-Za-z_$][A-Za-z0-9_$]*$");
    private static final Path DEFAULT_SCRIPTS_DIRECTORY = Main.MOD_DIR.resolve("scripts");

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("template")
                .then(ClientCommandManager.argument(ID_ARG, StringArgumentType.string())
                        .then(ClientCommandManager.argument(NAME_ARG, StringArgumentType.string())
                                .then(ClientCommandManager.argument(VERSION_ARG, StringArgumentType.string())
                                        .executes(context -> createTemplate(context, null))
                                        .then(ClientCommandManager.argument(PATH_ARG, StringArgumentType.greedyString())
                                                .executes(context -> createTemplate(
                                                        context,
                                                        StringArgumentType.getString(context, PATH_ARG)
                                                ))))));
    }

    private int createTemplate(CommandContext<FabricClientCommandSource> context, String requestedPath) {
        String scriptId = StringArgumentType.getString(context, ID_ARG);
        String scriptName = StringArgumentType.getString(context, NAME_ARG);
        String scriptVersion = StringArgumentType.getString(context, VERSION_ARG);

        String validationError = validateArguments(scriptId, scriptName, scriptVersion);
        if (validationError != null) {
            ChatUtils.addErrorChatMessage(validationError, true);
            return CommandManager.COMMAND_FAILURE;
        }

        try {
            Path targetDirectory = resolveTargetDirectory(requestedPath);
            Files.createDirectories(targetDirectory);

            Path targetFile = targetDirectory.resolve(scriptId.toLowerCase(Locale.ROOT) + ".js");
            writeTemplate(targetFile, buildTemplateContent(scriptId, scriptName, scriptVersion));

            ChatUtils.addSuccessChatMessage("Created script template: " + formatPath(targetFile), true);
            return CommandManager.COMMAND_SUCCESS;
        } catch (IllegalArgumentException exception) {
            ChatUtils.addErrorChatMessage(exception.getMessage(), true);
            return CommandManager.COMMAND_FAILURE;
        } catch (FileAlreadyExistsException exception) {
            ChatUtils.addErrorChatMessage("Script template already exists: " + formatPath(exception.getFile()), true);
            return CommandManager.COMMAND_FAILURE;
        } catch (IOException exception) {
            Main.LOGGER.atError().setCause(exception).log("Failed to create script template.");
            ChatUtils.addErrorChatMessage("Failed to create script template. Check logs for details.", true);
            return CommandManager.COMMAND_FAILURE;
        }
    }

    private String validateArguments(String scriptId, String scriptName, String scriptVersion) {
        if (!VALID_SCRIPT_ID_PATTERN.matcher(scriptId).matches()) {
            return "Invalid script id. Use a valid JavaScript class name (letters, digits, _, $ and no leading digit).";
        }
        if (scriptName.isBlank()) {
            return "Script name cannot be empty.";
        }
        if (scriptVersion.isBlank()) {
            return "Script version cannot be empty.";
        }
        if (scriptName.contains(",") || scriptVersion.contains(",")) {
            return "Script name and version cannot contain commas.";
        }
        return null;
    }

    private Path resolveTargetDirectory(String requestedPath) {
        String sanitizedPath = requestedPath == null ? "" : requestedPath.trim();
        Path targetDirectory;
        if (sanitizedPath.isEmpty()) {
            targetDirectory = DEFAULT_SCRIPTS_DIRECTORY.toAbsolutePath().normalize();
        } else {
            try {
                targetDirectory = PathUtils.resolvePathFromModDir(sanitizedPath);
            } catch (InvalidPathException exception) {
                throw new IllegalArgumentException("Invalid path: " + sanitizedPath, exception);
            }
        }

        if (Files.exists(targetDirectory) && !Files.isDirectory(targetDirectory)) {
            throw new IllegalArgumentException("Path is not a directory: " + targetDirectory);
        }

        return targetDirectory;
    }

    private void writeTemplate(Path targetFile, String content) throws IOException {
        Files.writeString(targetFile, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private String buildTemplateContent(String scriptId, String scriptName, String scriptVersion) {
        return String.format(
                Locale.ROOT,
                "// @script(id=%s, name=%s, version=%s)%n"
                        + "class %s {%n"
                        + "    onEnable() {%n"
                        + "        // triggered when the script is enabled%n"
                        + "    }%n"
                        + "%n"
                        + "    onDisable() {%n"
                        + "        // triggered when the script is disabled%n"
                        + "    }%n"
                        + "}%n"
                        + "%n"
                        + "exportScript(%s);%n",
                scriptId,
                scriptName,
                scriptVersion,
                scriptId,
                scriptId
        );
    }

    private String formatPath(Path path) {
        return PathUtils.formatPathRelativeToModDir(path);
    }

    private String formatPath(String path) {
        if (path == null || path.isBlank()) {
            return "unknown path";
        }
        return formatPath(Path.of(path));
    }
}
