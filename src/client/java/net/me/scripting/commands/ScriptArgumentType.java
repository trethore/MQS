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

package net.me.scripting.commands;

import com.mojang.brigadier.arguments.*;

import java.util.Arrays;
import java.util.function.Supplier;

public enum ScriptArgumentType {
    WORD("word", StringArgumentType::word),
    STRING("string", StringArgumentType::string),
    GREEDY_STRING("greedy", "text", StringArgumentType::greedyString),
    INTEGER("integer", "int", IntegerArgumentType::integer),
    LONG("long", LongArgumentType::longArg),
    FLOAT("float", FloatArgumentType::floatArg),
    DOUBLE("double", DoubleArgumentType::doubleArg),
    BOOLEAN("boolean", "bool", BoolArgumentType::bool);

    private final String[] names;
    private final Supplier<ArgumentType<?>> supplier;


    ScriptArgumentType(String name, Supplier<ArgumentType<?>> supplier) {
        this(new String[]{name}, supplier);
    }

    ScriptArgumentType(String name, String alias, Supplier<ArgumentType<?>> supplier) {
        this(new String[]{name, alias}, supplier);
    }

    ScriptArgumentType(String[] names, Supplier<ArgumentType<?>> supplier) {
        this.names = names;
        this.supplier = supplier;
    }

    public static ScriptArgumentType fromString(String name) {
        for (ScriptArgumentType type : values()) {
            for (String typeName : type.names) {
                if (typeName.equalsIgnoreCase(name)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown argument type: " + name + ". Available: " + Arrays.toString(values()));
    }

    public ArgumentType<?> get() {
        return supplier.get();
    }

    @Override
    public String toString() {
        return names[0];
    }
}