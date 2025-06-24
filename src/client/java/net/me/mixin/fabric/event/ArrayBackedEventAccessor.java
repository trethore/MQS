package net.me.mixin.fabric.event;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(targets = "net.fabricmc.fabric.impl.base.event.ArrayBackedEvent", remap = false)
public interface ArrayBackedEventAccessor<T> {

    @Accessor("lock")
    Object getLock();

    @Accessor("phases")
    Map<Identifier, T> getPhases();

    @Accessor("handlers")
    T[] getHandlers();

    @Invoker("rebuildInvoker")
    void callRebuildInvoker(int newLength);
}