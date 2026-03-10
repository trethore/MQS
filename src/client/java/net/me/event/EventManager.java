/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Réthoré
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

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.event.events.tick.EndClientTickEvent;
import net.me.event.events.tick.StartClientTickEvent;
import net.me.scripting.ScriptManager;
import net.me.scripting.script.RunningScript;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

    private final Map<Class<? extends Event>, Map<EventPhase, List<Listener>>> listeners = new ConcurrentHashMap<>();
    private final ScriptManager scriptManager;
    private final FabricEventAdapter fabricAdapter;

    public EventManager(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        this.fabricAdapter = new FabricEventAdapter(scriptManager);

        ClientTickEvents.START_CLIENT_TICK.register(client -> this.post(new StartClientTickEvent(client)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.post(new EndClientTickEvent(client)));
    }

    public void post(Event event) {
        Map<EventPhase, List<Listener>> phaseListeners = listeners.get(event.getClass());
        if (phaseListeners == null || phaseListeners.isEmpty()) {
            return;
        }

        executePhase(phaseListeners, event, EventPhase.PRE);

        if (event instanceof CancellableEvent cancellable && cancellable.isCancelled()) {
            return;
        }

        executePhase(phaseListeners, event, EventPhase.POST);
    }

    private void executePhase(Map<EventPhase, List<Listener>> phaseListeners, Event event, EventPhase phase) {
        List<Listener> phaseSpecificListeners = phaseListeners.get(phase);
        if (phaseSpecificListeners == null) {
            return;
        }
        for (Listener listener : phaseSpecificListeners) {
            executeListener(listener, event, phase);
        }
    }

    private void executeListener(Listener listener, Event event, EventPhase phase) {
        RunningScript previousScript = scriptManager.getCurrentScript();
        scriptManager.setCurrentScript(listener.owner());
        try {
            listener.callback().execute(event);
        } catch (Exception e) {
            LOGGER.error("Error executing event listener for {} in script '{}' during phase {}",
                    event.getClass().getSimpleName(), listener.owner().getName(), phase, e);
        } finally {
            scriptManager.setCurrentScript(previousScript);
        }
    }

    public void register(RunningScript owner, Class<? extends Event> eventType, Value callback) {
        register(owner, eventType, EventPhase.POST, callback);
    }

    public void register(RunningScript owner, Class<? extends Event> eventType, EventPhase phase, Value callback) {
        listeners.computeIfAbsent(eventType, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(phase, ignored -> new CopyOnWriteArrayList<>())
                .add(new Listener(owner, callback));
    }

    public void register(RunningScript owner, Events eventEnum, Value callback) {
        register(owner, eventEnum.getEventClass(), EventPhase.POST, callback);
    }

    public void register(RunningScript owner, Events eventEnum, EventPhase phase, Value callback) {
        register(owner, eventEnum.getEventClass(), phase, callback);
    }

    public void registerFabric(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent, Value jsCallback) {
        fabricAdapter.register(owner, fabricEvent, jsCallback);
    }

    public void unregisterAll(RunningScript owner) {
        listeners.values().forEach(phaseMap ->
                phaseMap.values().forEach(list ->
                        list.removeIf(listener -> listener.owner().equals(owner))
                )
        );
        fabricAdapter.unregisterAll(owner);
    }

    public void unregister(RunningScript owner, Class<? extends Event> eventType) {
        Map<EventPhase, List<Listener>> phaseMap = listeners.get(eventType);
        if (phaseMap != null) {
            phaseMap.values().forEach(list -> list.removeIf(listener -> listener.owner().equals(owner)));
        }
    }

    public void unregister(RunningScript owner, Events eventEnum) {
        unregister(owner, eventEnum.getEventClass());
    }

    public void unregister(RunningScript owner, Class<? extends Event> eventType, Value callback) {
        Map<EventPhase, List<Listener>> phaseMap = listeners.get(eventType);
        if (phaseMap != null) {
            phaseMap.values().forEach(list -> list.removeIf(listener -> listener.owner().equals(owner) && listener.callback().equals(callback)));
        }
    }

    public void unregister(RunningScript owner, Events eventEnum, Value callback) {
        unregister(owner, eventEnum.getEventClass(), callback);
    }

    public void unregister(RunningScript owner, Class<? extends Event> eventType, EventPhase phase) {
        Map<EventPhase, List<Listener>> phaseMap = listeners.get(eventType);
        if (phaseMap != null) {
            List<Listener> list = phaseMap.get(phase);
            if (list != null) {
                list.removeIf(listener -> listener.owner().equals(owner));
            }
        }
    }

    public void unregister(RunningScript owner, Events eventEnum, EventPhase phase) {
        unregister(owner, eventEnum.getEventClass(), phase);
    }

    public void unregister(RunningScript owner, Class<? extends Event> eventType, EventPhase phase, Value callback) {
        Map<EventPhase, List<Listener>> phaseMap = listeners.get(eventType);
        if (phaseMap != null) {
            List<Listener> list = phaseMap.get(phase);
            if (list != null) {
                list.removeIf(listener -> listener.owner().equals(owner) && listener.callback().equals(callback));
            }
        }
    }

    public void unregister(RunningScript owner, Events eventEnum, EventPhase phase, Value callback) {
        unregister(owner, eventEnum.getEventClass(), phase, callback);
    }

    public void unregister(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent) {
        fabricAdapter.unregister(owner, fabricEvent);
    }

    public void unregister(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent, Value callback) {
        fabricAdapter.unregister(owner, fabricEvent, callback);
    }

    private record Listener(RunningScript owner, Value callback) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Listener that = (Listener) o;
            return owner.equals(that.owner) && callback.equals(that.callback);
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, callback);
        }
    }
}
