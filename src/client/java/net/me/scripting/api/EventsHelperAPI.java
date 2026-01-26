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

package net.me.scripting.api;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.event.EventManager;
import net.me.event.EventPhase;
import net.me.event.EventSubscriptionOptions;
import net.me.event.Events;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static net.me.scripting.api.ApiConstants.*;

public class EventsHelperAPI implements ProxyObject {
    private static final Set<String> MEMBER_KEYS = Set.of(
            FABRIC,
            OFF,
            UNREGISTER,
            OPTIONS
    );

    private final EventManager eventManager;
    private final ScriptManager scriptManager;
    private final Map<String, Events> namedEvents = new HashMap<>();
    private final ProxyObject fabricProxy;
    private final Map<RunningScript, Set<EventHandle>> handlesByScript = new ConcurrentHashMap<>();

    public EventsHelperAPI(EventManager eventManager, ScriptManager scriptManager) {
        this.eventManager = eventManager;
        this.scriptManager = scriptManager;
        this.fabricProxy = createFabricProxy();
        for (Events event : Events.values()) {
            namedEvents.put(buildHandlerName(event), event);
        }
    }

    @Override
    public Object getMember(String key) {
        if (FABRIC.equals(key)) {
            return fabricProxy;
        }
        if (OFF.equals(key)) {
            return createOffExecutable();
        }
        if (UNREGISTER.equals(key)) {
            return createUnregisterExecutable();
        }
        if (OPTIONS.equals(key)) {
            return createOptionsExecutable();
        }
        Events mappedEvent = namedEvents.get(key);
        if (mappedEvent == null) {
            return null;
        }
        return createEventExecutable(mappedEvent);
    }

    @Override
    public Object getMemberKeys() {
        String[] keys = new String[namedEvents.size() + MEMBER_KEYS.size()];
        int index = 0;
        for (String helper : MEMBER_KEYS) {
            keys[index++] = helper;
        }
        for (String name : namedEvents.keySet()) {
            keys[index++] = name;
        }
        return keys;
    }

    @Override
    public boolean hasMember(String key) {
        return MEMBER_KEYS.contains(key) || namedEvents.containsKey(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the MQS.events object.");
    }

    private ProxyObject createFabricProxy() {
        Map<String, net.fabricmc.fabric.api.event.Event<?>> fabricEvents = Map.of(
                "clientTickEnd", ClientTickEvents.END_CLIENT_TICK,
                "clientTickStart", ClientTickEvents.START_CLIENT_TICK
        );

        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                net.fabricmc.fabric.api.event.Event<?> fabricEvent = fabricEvents.get(key);
                if (fabricEvent == null) {
                    return null;
                }
                return (ProxyExecutable) args -> {
                    RunningScript owner = getCurrentScript();
                    Value callback = ApiArgumentChecks.requireExecutable(args, 0, "First argument must be a callback function.");
                    eventManager.registerFabric(owner, fabricEvent, callback);
                    return registerFabricHandle(owner, fabricEvent, callback);
                };
            }

            @Override
            public Object getMemberKeys() {
                return fabricEvents.keySet().toArray(new String[0]);
            }

            @Override
            public boolean hasMember(String key) {
                return fabricEvents.containsKey(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify MQS.events.fabric.");
            }
        };
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("Events helper can only be used from an active script.");
        }
        return script;
    }

    private String buildHandlerName(Events event) {
        String rawName = event.name();
        int idx = rawName.lastIndexOf("Event");
        if (idx >= 0) {
            rawName = rawName.substring(0, idx) + rawName.substring(idx + "Event".length());
        }
        return "on" + rawName;
    }

    private Value registerEventHandle(RunningScript owner, Events event, EventPhase phase, Value callback) {
        EventHandle handle = new EventHandle(owner, event, phase, callback, null);
        Value disposer = createDisposer(owner, handle);
        handle.attachDisposer(disposer);
        trackHandle(owner, handle);
        return disposer;
    }

    private Value registerFabricHandle(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent, Value callback) {
        EventHandle handle = new EventHandle(owner, null, null, callback, fabricEvent);
        Value disposer = createDisposer(owner, handle);
        handle.attachDisposer(disposer);
        trackHandle(owner, handle);
        return disposer;
    }

    private Value createDisposer(RunningScript owner, EventHandle handle) {
        ProxyExecutable exec = _ -> {
            handle.dispose();
            return null;
        };
        return owner.getContext().asValue(exec);
    }

    private void trackHandle(RunningScript owner, EventHandle handle) {
        handlesByScript.computeIfAbsent(owner, _ -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(handle);
    }

    private void disposeHandles(RunningScript owner, Predicate<EventHandle> predicate) {
        Set<EventHandle> owned = handlesByScript.get(owner);
        if (owned == null || owned.isEmpty()) {
            return;
        }
        List<EventHandle> snapshot = List.copyOf(owned);
        for (EventHandle handle : snapshot) {
            if (predicate.test(handle)) {
                handle.dispose();
            }
        }
    }

    private ProxyExecutable createOffExecutable() {
        return args -> {
            RunningScript owner = getCurrentScript();
            if (args.length == 0) {
                disposeAll(owner);
                return null;
            }
            Value target = args[0];
            if (target == null) {
                throw new IllegalArgumentException("off() requires a callback or disposer when provided.");
            }
            disposeHandles(owner, handle -> handle.matchesValue(target));
            return null;
        };
    }

    private void disposeAll(RunningScript owner) {
        disposeHandles(owner, _ -> true);
        eventManager.unregisterAll(owner);
        handlesByScript.remove(owner);
    }

    private ProxyExecutable createUnregisterExecutable() {
        return args -> {
            ApiArgumentChecks.requireArgCountAtLeast(args, 1, "unregister(eventOrCallback, phase?) requires at least one argument.");
            RunningScript owner = getCurrentScript();
            Value target = args[0];
            if (target != null && target.canExecute()) {
                disposeHandles(owner, handle -> handle.matchesValue(target));
                return null;
            }
            if (target != null && target.isHostObject() && target.asHostObject() instanceof Events eventEnum) {
                unregisterEvent(owner, eventEnum, resolveOptionalPhase(args));
                return null;
            }
            if (target != null && target.isString()) {
                unregisterByKey(owner, target.asString(), resolveOptionalPhase(args));
                return null;
            }
            throw new IllegalArgumentException("Unsupported unregister target. Pass an MQS event enum, disposer, or callback function.");
        };
    }

    private EventPhase resolveOptionalPhase(Value[] args) {
        if (args.length > 1 && args[1] != null) {
            return EventSubscriptionOptions.resolvePhaseValue(args[1]);
        }
        return null;
    }

    private void unregisterEvent(RunningScript owner, Events eventEnum, EventPhase phase) {
        disposeHandles(owner, handle -> handle.matchesEvent(eventEnum, phase));
        if (phase != null) {
            eventManager.unregister(owner, eventEnum, phase);
        } else {
            eventManager.unregister(owner, eventEnum);
        }
    }

    private void unregisterByKey(RunningScript owner, String eventKey, EventPhase phase) {
        Events mapped = namedEvents.get(eventKey);
        if (mapped == null) {
            throw new IllegalArgumentException("Unknown event key '" + eventKey + "'.");
        }
        unregisterEvent(owner, mapped, phase);
    }

    private ProxyExecutable createOptionsExecutable() {
        return _ -> getCurrentScript().getContext().asValue(EventSubscriptionOptions.builder());
    }

    private ProxyExecutable createEventExecutable(Events mappedEvent) {
        return args -> {
            RunningScript owner = getCurrentScript();
            Value callback = ApiArgumentChecks.requireExecutable(args, 0, "First argument must be a callback function.");
            EventSubscriptionOptions options = EventSubscriptionOptions.fromScript(args.length > 1 ? args[1] : null, EventPhase.POST);
            EventPhase phase = options.phase();
            eventManager.register(owner, mappedEvent, phase, callback);
            return registerEventHandle(owner, mappedEvent, phase, callback);
        };
    }

    private final class EventHandle {
        private final RunningScript owner;
        private final Events event;
        private final EventPhase phase;
        private final Value callback;
        private final net.fabricmc.fabric.api.event.Event<?> fabricEvent;
        private final AtomicBoolean disposed = new AtomicBoolean(false);
        private Value disposer;

        private EventHandle(RunningScript owner, Events event, EventPhase phase, Value callback, net.fabricmc.fabric.api.event.Event<?> fabricEvent) {
            this.owner = owner;
            this.event = event;
            this.phase = phase;
            this.callback = callback;
            this.fabricEvent = fabricEvent;
        }

        private void attachDisposer(Value disposer) {
            this.disposer = disposer;
        }

        private boolean matchesValue(Value candidate) {
            return valueEquals(candidate, callback) || valueEquals(candidate, disposer);
        }

        private boolean matchesEvent(Events targetEvent, EventPhase targetPhase) {
            if (!Objects.equals(event, targetEvent)) {
                return false;
            }
            if (targetPhase == null) {
                return true;
            }
            return Objects.equals(phase, targetPhase);
        }

        private void dispose() {
            if (!disposed.compareAndSet(false, true)) {
                return;
            }
            if (event != null) {
                if (phase != null) {
                    eventManager.unregister(owner, event, phase, callback);
                } else {
                    eventManager.unregister(owner, event, callback);
                }
            } else if (fabricEvent != null) {
                eventManager.unregister(owner, fabricEvent, callback);
            }
            Set<EventHandle> owned = handlesByScript.get(owner);
            if (owned != null) {
                owned.remove(this);
                if (owned.isEmpty()) {
                    handlesByScript.remove(owner);
                }
            }
        }

        private boolean valueEquals(Value a, Value b) {
            if (a == null || b == null) {
                return false;
            }
            if (a == b) {
                return true;
            }
            if (a.equals(b)) {
                return true;
            }
            if (a.isHostObject() && b.isHostObject()) {
                Object hostA = a.asHostObject();
                Object hostB = b.asHostObject();
                if (hostA == hostB) {
                    return true;
                }
            }
            if (a.isProxyObject() && b.isProxyObject() && a.asProxyObject() == b.asProxyObject()) {
                return true;
            }
            try {
                if (a.hashCode() == b.hashCode()) {
                    return true;
                }
            } catch (Exception _) {
                // Ignore hashCode exceptions
            }
            return false;
        }
    }
}
