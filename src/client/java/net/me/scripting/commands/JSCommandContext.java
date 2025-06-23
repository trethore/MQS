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