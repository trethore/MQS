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

package net.me.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.me.command.commands.ScriptCommand;
import net.me.command.commands.UpdateCommand;
import net.me.scripting.ScriptingService;

public class MQSCommand extends Command {
    private final ScriptCommand scriptCommand;
    private final UpdateCommand updateCommand;

    public MQSCommand(ScriptingService scriptingService) {
        this.scriptCommand = new ScriptCommand(scriptingService);
        this.updateCommand = new UpdateCommand();
    }

    @Override
    protected LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        return ClientCommandManager.literal("mqs")
                .requires(source -> source.hasPermissionLevel(0))
                .then(scriptCommand.buildCommand())
                .then(updateCommand.buildCommand());
    }
}
