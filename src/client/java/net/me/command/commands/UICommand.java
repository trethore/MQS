/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.Command;
import net.me.command.CommandManager;
import net.me.scripting.ScriptingService;
import net.me.ui.screen.screens.ScriptsMenuScreen;
import net.me.utils.McUtils;

public class UICommand extends Command {

    private final ScriptingService scriptingService;

    public UICommand(ScriptingService scriptingService) {
        this.scriptingService = scriptingService;
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("ui")
                .executes(context -> openUi());
    }

    private int openUi() {
        McUtils.getMc().ifPresent(mc -> mc.send(() -> new ScriptsMenuScreen(scriptingService).open()));
        return CommandManager.COMMAND_SUCCESS;
    }
}
