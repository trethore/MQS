package net.me.mixin.client.accessors;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(CommandNode.class)
public interface CommandNodeAccessor<S> {

    @Accessor("children")
    Map<String, CommandNode<S>> getChildrenMap();

    @Accessor("literals")
    Map<String, LiteralCommandNode<S>> getLiteralsMap();
}