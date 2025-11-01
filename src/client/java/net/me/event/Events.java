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

package net.me.event;

import lombok.Getter;
import net.me.event.events.MinecraftClientStopEvent;
import net.me.event.events.chat.ChatMessageReceivedEvent;
import net.me.event.events.interact.BlockInteractEvent;
import net.me.event.events.interact.ItemUseEvent;
import net.me.event.events.packet.ClientPacketInputEvent;
import net.me.event.events.packet.ClientPacketOutputEvent;
import net.me.event.events.player.*;
import net.me.event.events.render.EntityRenderEvent;
import net.me.event.events.render.HudRenderEvent;
import net.me.event.events.render.NameTagRenderEvent;
import net.me.event.events.render.WorldRenderEvent;
import net.me.event.events.screen.*;
import net.me.event.events.screen.deathscreen.DeathScreenInitEvent;
import net.me.event.events.screen.titlescreen.TitleScreenInitEvent;
import net.me.event.events.sound.PlaySoundEvent;
import net.me.event.events.tick.ClientPlayerTickEvent;
import net.me.event.events.tick.EndClientTickEvent;
import net.me.event.events.tick.StartClientTickEvent;
import net.me.event.events.title.SubTitleEvent;
import net.me.event.events.title.TitleEvent;
import net.me.event.events.world.EntityAddedEvent;
import net.me.event.events.world.EntityRemovedEvent;
import net.me.event.events.world.WorldChangeEvent;

@Getter
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
    PlayerMoveEvent(PlayerMoveEvent.class),

    // World Interaction Events
    BlockInteractEvent(BlockInteractEvent.class),
    ItemUseEvent(ItemUseEvent.class),
    WorldChangeEvent(WorldChangeEvent.class),
    ChatMessageReceivedEvent(ChatMessageReceivedEvent.class),
    EntityAddedEvent(EntityAddedEvent.class),            // <-- ADD THIS
    EntityRemovedEvent(EntityRemovedEvent.class),
    // Screen Events
    ScreenFirstInitEvent(ScreenFirstInitEvent.class),
    ScreenInitEvent(ScreenInitEvent.class),
    ScreenDisplayedEvent(ScreenDisplayedEvent.class),
    ScreenRemovedEvent(ScreenRemovedEvent.class),
    ScreenCloseEvent(ScreenCloseEvent.class),
    // Title Screen Events
    TitleScreenInitEvent(TitleScreenInitEvent.class),
    // Death Screen Events
    DeathScreenInitEvent(DeathScreenInitEvent.class),

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

}