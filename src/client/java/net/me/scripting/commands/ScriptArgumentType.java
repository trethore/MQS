package net.me.scripting.commands;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

public enum ScriptArgumentType {
    WORD("word", StringArgumentType::word, c -> StringArgumentType.getString(c, "arg")),
    STRING("string", StringArgumentType::string, c -> StringArgumentType.getString(c, "arg")),
    GREEDY_STRING("greedy", "text", StringArgumentType::greedyString, c -> StringArgumentType.getString(c, "arg")),
    INTEGER("integer", "int", IntegerArgumentType::integer, c -> IntegerArgumentType.getInteger(c, "arg")),
    LONG("long", LongArgumentType::longArg, c -> LongArgumentType.getLong(c, "arg")),
    FLOAT("float", FloatArgumentType::floatArg, c -> FloatArgumentType.getFloat(c, "arg")),
    DOUBLE("double", DoubleArgumentType::doubleArg, c -> DoubleArgumentType.getDouble(c, "arg")),
    BOOLEAN("boolean", "bool", BoolArgumentType::bool, c -> BoolArgumentType.getBool(c, "arg"));

    private final String[] names;
    private final Supplier<ArgumentType<?>> supplier;


    ScriptArgumentType(String name, Supplier<ArgumentType<?>> supplier, Function<CommandContext<FabricClientCommandSource>, Object> getter) {
        this(new String[]{name}, supplier, getter);
    }

    ScriptArgumentType(String name, String alias, Supplier<ArgumentType<?>> supplier, Function<CommandContext<FabricClientCommandSource>, Object> getter) {
        this(new String[]{name, alias}, supplier, getter);
    }

    ScriptArgumentType(String[] names, Supplier<ArgumentType<?>> supplier, Function<CommandContext<FabricClientCommandSource>, Object> getter) {
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