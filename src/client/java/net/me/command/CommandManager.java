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

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.ArrayList;

public class CommandManager {

    public static final int COMMAND_SUCCESS = 1;
    public static final int COMMAND_FAILURE = -1;

    private final ArrayList<Command> commands = new ArrayList<>();

    public void init() {
        registerCommands();
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> registerClientCommands(dispatcher));
    }

    private void registerClientCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        registerCommandsInDispatcher(dispatcher);
    }

    private void registerCommandsInDispatcher(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        commands.forEach(command -> command.register(dispatcher));
    }

    public void addCommand(Command command) {
        this.commands.add(command);
    }
}
