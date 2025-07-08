package net.me.mixin.client.mixins;

import net.me.Main;
import net.me.event.events.sound.PlaySoundEvent;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public class MQSSoundSystemMixin {

    @Unique
    private final ThreadLocal<PlaySoundEvent> currentSoundEvent = new ThreadLocal<>();

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void onPlayHead(SoundInstance sound, CallbackInfo ci) {
        PlaySoundEvent event = new PlaySoundEvent(sound);
        Main.getInstance().getEventManager().post(event);

        if (event.isCancelled()) {
            ci.cancel();
        } else {
            currentSoundEvent.set(event);
        }
    }

    @ModifyVariable(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), argsOnly = true)
    private SoundInstance onPlayModifySound(SoundInstance originalSound) {
        PlaySoundEvent event = currentSoundEvent.get();
        currentSoundEvent.remove();

        if (event != null) {
            return event.getSoundInstance();
        }
        return originalSound;
    }
}