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

package net.me.console;

import lombok.Getter;

import java.util.Optional;

@Getter
public abstract class ConsoleCommand {
    private final ConsoleManager consoleManager;
    private final String name;
    private final String description;
    private final String usage;
    protected ConsoleCommand(ConsoleManager consoleManager, String name, String description, String usage) {
        this.consoleManager = consoleManager;
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    public abstract void execute(String[] args);

    protected Optional<Boolean> parseRequiredBooleanArg(String[] args) {
        if (args.length != 1) {
            consoleManager.logError("Invalid arguments. Usage: " + getUsage());
            return Optional.empty();
        }

        Optional<Boolean> parsedBoolean = ConsoleUtils.parseBooleanArg(args[0]);
        if (parsedBoolean.isEmpty()) {
            consoleManager.logError(
                    "Invalid argument '" + args[0] + "'. Must be '" + ConsoleUtils.TRUE_STRING + "' or '" + ConsoleUtils.FALSE_STRING + "'."
            );
        }
        return parsedBoolean;
    }

}
