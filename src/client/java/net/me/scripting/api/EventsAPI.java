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

package net.me.scripting.api;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.event.Event;
import net.me.event.EventManager;
import net.me.event.EventPhase;
import net.me.event.EventSubscriptionOptions;
import net.me.event.Events;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.internal.HandleTracker;
import net.me.scripting.api.internal.ScriptContextHelper;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.me.scripting.api.ApiConstants.*;

public class EventsAPI implements ProxyObject {
    private static final Set<String> MEMBER_KEYS = Set.of(
            FABRIC,
            OFF,
            UNREGISTER,
            OPTIONS,
            REGISTER,
            UNREGISTER_ALL,
            EVENTS,
            PHASE
    );

    private final EventManager eventManager;
    private final ScriptContextHelper contextHelper;
    private final HandleTracker<EventHandle> eventTracker;
    private final Map<String, Events> namedEvents = new HashMap<>();
    private final ProxyObject eventsEnumProxy = createEventsEnumProxy();
    private final ProxyObject eventPhaseEnumProxy = createEventPhaseEnumProxy();
    private final ProxyObject fabricProxy;

    public EventsAPI(EventManager eventManager, ScriptManager scriptManager) {
        this.eventManager = eventManager;
        this.contextHelper = new ScriptContextHelper(scriptManager);
        this.eventTracker = new HandleTracker<>();
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
        if (OPTIONS.equals(key)) {
            return createOptionsExecutable();
        }
        if (EVENTS.equals(key)) {
            return eventsEnumProxy;
        }
        if (PHASE.equals(key)) {
            return eventPhaseEnumProxy;
        }
        if (REGISTER.equals(key)) {
            return (ProxyExecutable) args -> register(contextHelper.require("Events API"), args);
        }
        if (UNREGISTER.equals(key)) {
            return (ProxyExecutable) args -> unregister(contextHelper.require("Events API"), args);
        }
        if (UNREGISTER_ALL.equals(key)) {
            return (ProxyExecutable) args -> unregisterAll(contextHelper.require("Events API"), args);
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

    private static Class<? extends Event> getEventClass(Object eventTarget) {
        return switch (eventTarget) {
            case Events eventEnum -> eventEnum.getEventClass();
            case Class<?> cls when Event.class.isAssignableFrom(cls) ->
                //noinspection unchecked
                    (Class<? extends Event>) cls;
            case null, default ->
                    throw new IllegalArgumentException("First argument to Events.unregister must be an MQS event class, a Fabric Event, or an MQS Event from Events.");
        };
    }

    private Object resolveEventTarget(Value eventTypeArg) {
        if (eventTypeArg == null) {
            throw new IllegalArgumentException("Event type cannot be null.");
        }

        Object unwrapped = ScriptUtils.unwrapReceiver(eventTypeArg);
        if (unwrapped instanceof JsClassWrapper wrapper) return wrapper.getTargetClass();
        if (unwrapped instanceof Class) return unwrapped;

        if (eventTypeArg.isProxyObject()) {
            Object proxy = eventTypeArg.asProxyObject();
            if (proxy instanceof LazyJsClassHolder holder) return holder.getWrapper().getTargetClass();
        }

        if (eventTypeArg.isHostObject()) {
            Object hostObject = eventTypeArg.asHostObject();
            if (hostObject instanceof Events) {
                return hostObject;
            }
            if (hostObject instanceof net.fabricmc.fabric.api.event.Event) return hostObject;
        }

        throw new IllegalArgumentException("Event target must be a class imported via importClass(), a direct Fabric Event object, or an MQS Event from Events.");
    }

    private EventPhase resolvePhase(Value phaseValue) {
        return EventSubscriptionOptions.resolvePhaseValue(phaseValue);
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
                    RunningScript owner = contextHelper.require("Events API");
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

    private String buildHandlerName(Events event) {
        String rawName = event.name();
        int idx = rawName.lastIndexOf("Event");
        if (idx >= 0) {
            rawName = rawName.substring(0, idx) + rawName.substring(idx + "Event".length());
        }
        return "on" + rawName;
    }

    private ProxyExecutable createOffExecutable() {
        return args -> {
            RunningScript owner = contextHelper.require("Events API");
            if (args.length == 0) {
                disposeAll(owner);
                return null;
            }
            Value target = args[0];
            if (target == null) {
                throw new IllegalArgumentException("off() requires a callback or disposer when provided.");
            }
            eventTracker.dispose(owner, handle -> handle.matchesValue(target), EventHandle::dispose);
            return null;
        };
    }

    private void disposeAll(RunningScript owner) {
        eventTracker.disposeAll(owner, EventHandle::dispose);
        eventManager.unregisterAll(owner);
    }

    private ProxyExecutable createOptionsExecutable() {
        return ignored -> contextHelper.require("Events API").getContext().asValue(EventSubscriptionOptions.builder());
    }

    private ProxyExecutable createEventExecutable(Events mappedEvent) {
        return args -> {
            RunningScript owner = contextHelper.require("Events API");
            Value callback = ApiArgumentChecks.requireExecutable(args, 0, "First argument must be a callback function.");
            EventSubscriptionOptions options = EventSubscriptionOptions.fromScript(args.length > 1 ? args[1] : null, EventPhase.POST);
            EventPhase phase = options.phase();
            eventManager.register(owner, mappedEvent, phase, callback);
            return registerEventHandle(owner, mappedEvent, phase, callback);
        };
    }

    private Value register(RunningScript owner, Value[] args) {
        ApiArgumentChecks.requireArgCountRange(args, 2, 3, "Usage: Events.register(EventType, [Phase], callbackFunction)");
        Registration registration = parseRegistration(args);
        return registerTarget(owner, registration);
    }

    private Registration parseRegistration(Value[] args) {
        Object eventTarget = resolveEventTarget(args[0]);
        if (args.length == 2) {
            Value callback = ApiArgumentChecks.requireExecutable(args, 1, "Callback must be a function.");
            return new Registration(eventTarget, EventPhase.POST, callback);
        }
        EventPhase phase = resolvePhase(args[1]);
        if (phase == null) {
            throw new IllegalArgumentException("Second argument must be a valid phase (PRE or POST).");
        }
        Value callback = ApiArgumentChecks.requireExecutable(args, 2, "Callback must be a function.");
        return new Registration(eventTarget, phase, callback);
    }

    private Value registerTarget(RunningScript owner, Registration registration) {
        switch (registration.eventTarget()) {
            case Events eventEnum -> {
                eventManager.register(owner, eventEnum, registration.phase(), registration.callback());
                return registerEventHandle(owner, eventEnum, registration.phase(), registration.callback());
            }
            case Class<?> cls when Event.class.isAssignableFrom(cls) -> {
                //noinspection unchecked
                Class<? extends Event> eventClass = (Class<? extends Event>) cls;
                eventManager.register(owner, eventClass, registration.phase(), registration.callback());
                return registerEventClassHandle(owner, eventClass, registration.phase(), registration.callback());
            }
            case net.fabricmc.fabric.api.event.Event<?> fabricEvent -> {
                eventManager.registerFabric(owner, fabricEvent, registration.callback());
                return registerFabricHandle(owner, fabricEvent, registration.callback());
            }
            case null, default ->
                    throw new IllegalArgumentException("First argument to Events.register must be a MQS event class, a Fabric Event object, or an MQS Event from Events.");
        }
    }

    private Void unregisterAll(RunningScript owner, Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 0, "Usage: Events.unregisterAll()");
        eventTracker.disposeAll(owner, EventHandle::dispose);
        eventManager.unregisterAll(owner);
        return null;
    }

    private Void unregister(RunningScript owner, Value[] args) {
        ApiArgumentChecks.requireArgCountAtLeast(args, 1, "Usage: Events.unregister(EventType, [Phase|callback], [callback])");
        Value target = args[0];
        if (target != null && target.canExecute()) {
            eventTracker.dispose(owner, handle -> handle.matchesValue(target), EventHandle::dispose);
            return null;
        }
        if (target != null && target.isString()) {
            unregisterByKey(owner, target.asString(), resolveOptionalPhase(args));
            return null;
        }
        UnregisterCall call = parseUnregister(args);
        unregisterTarget(owner, call);
        return null;
    }

    private UnregisterCall parseUnregister(Value[] args) {
        Object eventTarget = resolveEventTarget(args[0]);
        UnregisterFilters filters = parseUnregisterFilters(args);
        return new UnregisterCall(eventTarget, filters.phase(), filters.callback());
    }

    private UnregisterFilters parseUnregisterFilters(Value[] args) {
        if (args.length <= 1) {
            return new UnregisterFilters(null, null);
        }
        EventPhase potentialPhase = resolvePhase(args[1]);
        if (potentialPhase != null) {
            Value callback = args.length > 2
                    ? ApiArgumentChecks.requireExecutable(args, 2, "Third argument must be a callback function.")
                    : null;
            return new UnregisterFilters(potentialPhase, callback);
        }
        if (args[1].canExecute()) {
            if (args.length > 2) {
                throw new IllegalArgumentException("Cannot provide a third argument when the second is a callback.");
            }
            return new UnregisterFilters(null, args[1]);
        }
        throw new IllegalArgumentException("Second argument must be a Phase or a callback function.");
    }

    private EventPhase resolveOptionalPhase(Value[] args) {
        if (args.length > 1 && args[1] != null) {
            return EventSubscriptionOptions.resolvePhaseValue(args[1]);
        }
        return null;
    }

    private void unregisterByKey(RunningScript owner, String eventKey, EventPhase phase) {
        Events mapped = namedEvents.get(eventKey);
        if (mapped == null) {
            throw new IllegalArgumentException("Unknown event key '" + eventKey + "'.");
        }
        unregisterEvent(owner, mapped, phase);
    }

    private void unregisterTarget(RunningScript owner, UnregisterCall call) {
        if (call.eventTarget() instanceof net.fabricmc.fabric.api.event.Event<?> fabricEvent) {
            if (call.phase() != null) {
                throw new IllegalArgumentException("Fabric events do not support phases.");
            }
            if (call.callback() != null) {
                eventManager.unregister(owner, fabricEvent, call.callback());
            } else {
                eventManager.unregister(owner, fabricEvent);
            }
            eventTracker.dispose(owner, handle -> handle.matchesFabric(fabricEvent, call.callback()), EventHandle::dispose);
            return;
        }

        Class<? extends Event> eventType = getEventClass(call.eventTarget());
        unregisterMqsEvent(owner, eventType, call.phase(), call.callback());
        eventTracker.dispose(owner, handle -> handle.matchesEventClass(eventType, call.phase(), call.callback()), EventHandle::dispose);
    }

    private void unregisterEvent(RunningScript owner, Events eventEnum, EventPhase phase) {
        Class<? extends Event> eventClass = eventEnum.getEventClass();
        eventTracker.dispose(owner, handle -> handle.matchesEvent(eventEnum, phase) || handle.matchesEventClass(eventClass, phase, null), EventHandle::dispose);
        if (phase != null) {
            eventManager.unregister(owner, eventEnum, phase);
        } else {
            eventManager.unregister(owner, eventEnum);
        }
    }

    private void unregisterMqsEvent(RunningScript owner, Class<? extends Event> eventType, EventPhase phase, Value callback) {
        if (phase != null && callback != null) {
            eventManager.unregister(owner, eventType, phase, callback);
            return;
        }
        if (phase != null) {
            eventManager.unregister(owner, eventType, phase);
            return;
        }
        if (callback != null) {
            eventManager.unregister(owner, eventType, callback);
            return;
        }
        eventManager.unregister(owner, eventType);
    }

    private Value registerEventHandle(RunningScript owner, Events event, EventPhase phase, Value callback) {
        EventHandle handle = new EventHandle(owner, event, event.getEventClass(), phase, callback, null);
        Value disposer = contextHelper.createIdempotentDisposer(owner, () -> {
            handle.dispose();
            eventTracker.remove(owner, handle);
        });
        handle.attachDisposer(disposer);
        eventTracker.track(owner, handle);
        return disposer;
    }

    private Value registerEventClassHandle(RunningScript owner, Class<? extends Event> eventClass, EventPhase phase, Value callback) {
        EventHandle handle = new EventHandle(owner, null, eventClass, phase, callback, null);
        Value disposer = contextHelper.createIdempotentDisposer(owner, () -> {
            handle.dispose();
            eventTracker.remove(owner, handle);
        });
        handle.attachDisposer(disposer);
        eventTracker.track(owner, handle);
        return disposer;
    }

    private Value registerFabricHandle(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent, Value callback) {
        EventHandle handle = new EventHandle(owner, null, null, null, callback, fabricEvent);
        Value disposer = contextHelper.createIdempotentDisposer(owner, () -> {
            handle.dispose();
            eventTracker.remove(owner, handle);
        });
        handle.attachDisposer(disposer);
        eventTracker.track(owner, handle);
        return disposer;
    }

    private ProxyObject createEventsEnumProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                try {
                    return Events.valueOf(key);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }

            @Override
            public Object getMemberKeys() {
                return Arrays.stream(Events.values()).map(Enum::name).toArray(String[]::new);
            }

            @Override
            public boolean hasMember(String key) {
                return Arrays.stream(Events.values()).anyMatch(e -> e.name().equals(key));
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify the Events enum object.");
            }
        };
    }

    private ProxyObject createEventPhaseEnumProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                try {
                    return EventPhase.valueOf(key);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }

            @Override
            public Object getMemberKeys() {
                return Arrays.stream(EventPhase.values()).map(Enum::name).toArray(String[]::new);
            }

            @Override
            public boolean hasMember(String key) {
                return Arrays.stream(EventPhase.values()).anyMatch(e -> e.name().equals(key));
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify the EventPhase enum object.");
            }
        };
    }

    private final class EventHandle {
        private final RunningScript owner;
        private final Events event;
        private final Class<? extends Event> eventClass;
        private final EventPhase phase;
        private final Value callback;
        private final net.fabricmc.fabric.api.event.Event<?> fabricEvent;
        private final AtomicBoolean disposed = new AtomicBoolean(false);
        private Value disposer;

        private EventHandle(RunningScript owner, Events event, Class<? extends Event> eventClass, EventPhase phase, Value callback, net.fabricmc.fabric.api.event.Event<?> fabricEvent) {
            this.owner = owner;
            this.event = event;
            this.eventClass = eventClass;
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

        private boolean matchesEventClass(Class<? extends Event> targetClass, EventPhase targetPhase, Value targetCallback) {
            if (!Objects.equals(eventClass, targetClass)) {
                return false;
            }
            if (targetPhase != null && !Objects.equals(phase, targetPhase)) {
                return false;
            }
            if (targetCallback != null) {
                return valueEquals(callback, targetCallback);
            }
            return true;
        }

        private boolean matchesFabric(net.fabricmc.fabric.api.event.Event<?> targetEvent, Value targetCallback) {
            if (!Objects.equals(fabricEvent, targetEvent)) {
                return false;
            }
            if (targetCallback != null) {
                return valueEquals(callback, targetCallback);
            }
            return true;
        }

        private void dispose() {
            if (!disposed.compareAndSet(false, true)) {
                return;
            }
            if (fabricEvent != null) {
                eventManager.unregister(owner, fabricEvent, callback);
                return;
            }
            if (eventClass != null) {
                if (phase != null) {
                    eventManager.unregister(owner, eventClass, phase, callback);
                } else {
                    eventManager.unregister(owner, eventClass, callback);
                }
                return;
            }
            if (event != null) {
                if (phase != null) {
                    eventManager.unregister(owner, event, phase, callback);
                } else {
                    eventManager.unregister(owner, event, callback);
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
            } catch (Exception ignored) {
                return false;
            }
            return false;
        }
    }

    private record Registration(Object eventTarget, EventPhase phase, Value callback) {
    }

    private record UnregisterFilters(EventPhase phase, Value callback) {
    }

    private record UnregisterCall(Object eventTarget, EventPhase phase, Value callback) {
    }
}
