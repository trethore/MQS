package net.me.event.events.sound;

import net.me.event.Events;
import net.minecraft.client.sound.SoundInstance;

public class PlaySoundEvent extends SoundEvent {

    public PlaySoundEvent(SoundInstance soundInstance) {
        super(soundInstance);
    }

    @Override
    public Events getType() {
        return Events.PlaySoundEvent;
    }
}
