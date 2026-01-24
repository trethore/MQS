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
    WORLD_RENDER_EVENT(WorldRenderEvent.class),
    HUD_RENDER_EVENT(HudRenderEvent.class),
    ENTITY_RENDER_EVENT_PRE(EntityRenderEvent.Pre.class),
    ENTITY_RENDER_EVENT_POST(EntityRenderEvent.Post.class),
    NAME_TAG_RENDER_EVENT(NameTagRenderEvent.class),

    // Player & Combat Events
    PLAYER_ATTACK_ENTITY_EVENT(PlayerAttackEntityEvent.class),
    PLAYER_INTERACT_ENTITY_EVENT(PlayerInteractEntityEvent.class),
    PLAYER_DAMAGE_EVENT(PlayerDamageEvent.class),
    PLAYER_RESPAWN_EVENT(PlayerRespawnEvent.class),
    PLAYER_MOVE_EVENT(PlayerMoveEvent.class),

    // World Interaction Events
    BLOCK_INTERACT_EVENT(BlockInteractEvent.class),
    ITEM_USE_EVENT(ItemUseEvent.class),
    WORLD_CHANGE_EVENT(WorldChangeEvent.class),
    CHAT_MESSAGE_RECEIVED_EVENT(ChatMessageReceivedEvent.class),
    ENTITY_ADDED_EVENT(EntityAddedEvent.class),
    ENTITY_REMOVED_EVENT(EntityRemovedEvent.class),

    // Screen Events
    SCREEN_FIRST_INIT_EVENT(ScreenFirstInitEvent.class),
    SCREEN_INIT_EVENT(ScreenInitEvent.class),
    SCREEN_DISPLAYED_EVENT(ScreenDisplayedEvent.class),
    SCREEN_REMOVED_EVENT(ScreenRemovedEvent.class),
    SCREEN_CLOSE_EVENT(ScreenCloseEvent.class),

    // Title Screen Events
    TITLE_SCREEN_INIT_EVENT(TitleScreenInitEvent.class),

    // Death Screen Events
    DEATH_SCREEN_INIT_EVENT(DeathScreenInitEvent.class),

    // Sound Events
    PLAY_SOUND_EVENT(PlaySoundEvent.class),

    // Tick Events
    START_CLIENT_TICK_EVENT(StartClientTickEvent.class),
    END_CLIENT_TICK_EVENT(EndClientTickEvent.class),
    CLIENT_PLAYER_TICK_EVENT(ClientPlayerTickEvent.class),

    // Other Client Events
    MINECRAFT_CLIENT_STOP_EVENT(MinecraftClientStopEvent.class),

    // Packet Events
    CLIENT_PACKET_OUTPUT_EVENT(ClientPacketOutputEvent.class),
    CLIENT_PACKET_INPUT_EVENT(ClientPacketInputEvent.class),
    TITLE_EVENT(TitleEvent.class),
    SUBTITLE_EVENT(SubTitleEvent.class);

    private final Class<? extends Event> eventClass;

    Events(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

}