package net.me.event;

import net.me.event.events.MinecraftClientStopEvent;
import net.me.event.events.chat.ChatMessageReceivedEvent;
import net.me.event.events.interact.BlockInteractEvent;
import net.me.event.events.interact.ItemUseEvent;
import net.me.event.events.packet.ClientPacketInputEvent;
import net.me.event.events.packet.ClientPacketOutputEvent;
import net.me.event.events.player.PlayerAttackEntityEvent;
import net.me.event.events.player.PlayerDamageEvent;
import net.me.event.events.player.PlayerInteractEntityEvent;
import net.me.event.events.player.PlayerRespawnEvent;
import net.me.event.events.render.EntityRenderEvent;
import net.me.event.events.render.HudRenderEvent;
import net.me.event.events.render.NameTagRenderEvent;
import net.me.event.events.render.WorldRenderEvent;
import net.me.event.events.screen.ScreenCloseEvent;
import net.me.event.events.screen.ScreenDisplayedEvent;
import net.me.event.events.screen.ScreenInitEvent;
import net.me.event.events.screen.ScreenRemovedEvent;
import net.me.event.events.sound.PlaySoundEvent;
import net.me.event.events.tick.ClientPlayerTickEvent;
import net.me.event.events.tick.EndClientTickEvent;
import net.me.event.events.tick.StartClientTickEvent;
import net.me.event.events.title.SubTitleEvent;
import net.me.event.events.title.TitleEvent;
import net.me.event.events.world.WorldChangeEvent;

public enum Events {
    // Render Events
    WorldRenderEvent(WorldRenderEvent.class),
    HudRenderEvent(HudRenderEvent.class),
    EntityRenderEventPre(EntityRenderEvent.Pre.class),
    EntityRenderEventPost(EntityRenderEvent.Post.class),
    NameTagRenderEvent(NameTagRenderEvent.class),

    // Player & Combat Events
    PlayerAttackEntityEvent(PlayerAttackEntityEvent.class),
    PlayerInteractEntityEvent(PlayerInteractEntityEvent.class),
    PlayerDamageEvent(PlayerDamageEvent.class),
    PlayerRespawnEvent(PlayerRespawnEvent.class),

    // World Interaction Events
    BlockInteractEvent(BlockInteractEvent.class),
    ItemUseEvent(ItemUseEvent.class),
    WorldChangeEvent(WorldChangeEvent.class),
    ChatMessageReceivedEvent(ChatMessageReceivedEvent.class),

    // Screen Events
    ScreenInitEvent(ScreenInitEvent.class),
    ScreenDisplayedEvent(ScreenDisplayedEvent.class),
    ScreenRemovedEvent(ScreenRemovedEvent.class),
    ScreenCloseEvent(ScreenCloseEvent.class),

    // Sound Events
    PlaySoundEvent(PlaySoundEvent.class),

    // Tick Events
    StartClientTickEvent(StartClientTickEvent.class),
    EndClientTickEvent(EndClientTickEvent.class),
    ClientPlayerTickEvent(ClientPlayerTickEvent.class),

    // Other Client Events
    MinecraftClientStopEvent(MinecraftClientStopEvent.class),

    // Packet Events
    ClientPacketOutputEvent(ClientPacketOutputEvent.class),
    ClientPacketInputEvent(ClientPacketInputEvent.class),
    TitleEvent(TitleEvent.class),
    SubtitleEvent(SubTitleEvent.class);

    private final Class<? extends Event> eventClass;

    Events(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }
}