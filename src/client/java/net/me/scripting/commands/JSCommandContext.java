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

package net.me.scripting.commands;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.graalvm.polyglot.HostAccess;

@SuppressWarnings("unused")
public class JSCommandContext {
    private final CommandContext<FabricClientCommandSource> context;

    public JSCommandContext(CommandContext<FabricClientCommandSource> context) {
        this.context = context;
    }

    @HostAccess.Export
    public FabricClientCommandSource getSource() {
        return context.getSource();
    }

    @HostAccess.Export
    public String getArgumentAsString(String name) {
        return StringArgumentType.getString(context, name);
    }

    @HostAccess.Export
    public int getArgumentAsInt(String name) {
        return IntegerArgumentType.getInteger(context, name);
    }

    @HostAccess.Export
    public boolean getArgumentAsBool(String name) {
        return BoolArgumentType.getBool(context, name);
    }

    @HostAccess.Export
    public double getArgumentAsDouble(String name) {
        return DoubleArgumentType.getDouble(context, name);
    }

    @HostAccess.Export
    public float getArgumentAsFloat(String name) {
        return FloatArgumentType.getFloat(context, name);
    }

    @HostAccess.Export
    public long getArgumentAsLong(String name) {
        return LongArgumentType.getLong(context, name);
    }
}