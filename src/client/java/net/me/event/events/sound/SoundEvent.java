package net.me.event.events.sound;

import net.me.event.CancellableEvent;
import net.minecraft.client.sound.SoundInstance;

@SuppressWarnings("unused")
public abstract class SoundEvent extends CancellableEvent {
    private SoundInstance soundInstance;

    public SoundEvent(SoundInstance soundInstance) {
        this.soundInstance = soundInstance;
    }

    public SoundInstance getSoundInstance() {
        return soundInstance;
    }

    public void setSoundInstance(SoundInstance soundInstance) {
        this.soundInstance = soundInstance;
    }
}