package net.me.mixin.fabric.event;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(targets = "net.fabricmc.fabric.impl.base.event.EventPhaseData", remap = false)
public interface EventPhaseDataAccessor<T> {

    @Accessor
    T[] getListeners();


    @Accessor
    void setListeners(T[] listeners);
}