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

import net.me.event.*;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;
import java.util.Set;

import static net.me.scripting.api.ApiConstants.*;

public class EventAPI implements ProxyObject {

    private static final Set<String> MEMBER_KEYS = Set.of(REGISTER, UNREGISTER, UNREGISTER_ALL, EVENTS, PHASE);
    private final EventManager eventManager;
    private final ScriptManager scriptManager;

    private final ProxyObject eventsEnumProxy = createEventsEnumProxy();
    private final ProxyObject eventPhaseEnumProxy = createEventPhaseEnumProxy();

    public EventAPI(EventManager eventManager, ScriptManager scriptManager) {
        this.eventManager = eventManager;
        this.scriptManager = scriptManager;
    }

    private static Class<? extends Event> getEventClass(Object eventTarget) {
        return switch (eventTarget) {
            case Events eventEnum -> eventEnum.getEventClass();
            case Class<?> cls when Event.class.isAssignableFrom(cls) ->
                //noinspection unchecked
                    (Class<? extends Event>) cls;
            case null, default ->
                    throw new IllegalArgumentException("First argument to EventManager.unregister must be an MQS event class, a Fabric Event, or an MQS Event from EventManager.Events.");
        };
    }

    private RunningScript getCurrentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("EventManager can only be used inside onEnable/onDisable or a registered event callback.");
        }
        return script;
    }

    @Override
    public Object getMember(String key) {
        if (EVENTS.equals(key)) {
            return eventsEnumProxy;
        }
        if (PHASE.equals(key)) {
            return eventPhaseEnumProxy;
        }

        return (ProxyExecutable) args -> executeOperation(key, args, getCurrentScript());
    }

    private Object resolveEventTarget(Value eventTypeArg) {
        if (eventTypeArg == null) {
            throw new IllegalArgumentException("Event type cannot be null.");
        }

        Object unwrapped = ScriptUtils.unwrapReceiver(eventTypeArg);
        if (unwrapped instanceof JsClassWrapper wrapper) return wrapper.getTargetClass();
        if (unwrapped instanceof Class) return unwrapped; // Already a class

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

        throw new IllegalArgumentException("Event target must be a class imported via importClass(), a direct Fabric Event object, or an MQS Event from EventManager.Events.");
    }

    private EventPhase resolvePhase(Value phaseValue) {
        return EventSubscriptionOptions.resolvePhaseValue(phaseValue);
    }

    @Override
    public Object getMemberKeys() {
        return MEMBER_KEYS.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return MEMBER_KEYS.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the EventManager object.");
    }

    private Object executeOperation(String key, Value[] args, RunningScript owner) {
        return switch (key) {
            case REGISTER -> register(owner, args);
            case UNREGISTER_ALL -> unregisterAll(owner, args);
            case UNREGISTER -> unregister(owner, args);
            default -> throw new UnsupportedOperationException("Unsupported EventManager operation: " + key);
        };
    }

    private Void register(RunningScript owner, Value[] args) {
        ApiArgumentChecks.requireArgCountRange(args, 2, 3, "Usage: EventManager.register(EventType, [Phase], callbackFunction)");
        Registration registration = parseRegistration(args);
        registerTarget(owner, registration);
        return null;
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

    private void registerTarget(RunningScript owner, Registration registration) {
        switch (registration.eventTarget()) {
            case Events eventEnum ->
                    eventManager.register(owner, eventEnum, registration.phase(), registration.callback());
            case Class<?> cls when Event.class.isAssignableFrom(cls) ->
                //noinspection unchecked
                    eventManager.register(owner, (Class<? extends Event>) cls, registration.phase(), registration.callback());
            case net.fabricmc.fabric.api.event.Event<?> fabricEvent ->
                    eventManager.registerFabric(owner, fabricEvent, registration.callback());
            case null, default ->
                    throw new IllegalArgumentException("First argument to EventManager.register must be a MQS event class, a Fabric Event object, or an MQS Event from EventManager.Events.");
        }
    }

    private Void unregisterAll(RunningScript owner, Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 0, "Usage: EventManager.unregisterAll()");
        eventManager.unregisterAll(owner);
        return null;
    }

    private Void unregister(RunningScript owner, Value[] args) {
        ApiArgumentChecks.requireArgCountRange(args, 1, 3, "Usage: EventManager.unregister(EventType, [Phase|callback], [callback])");
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
            return;
        }

        Class<? extends Event> eventType = getEventClass(call.eventTarget());
        unregisterMqsEvent(owner, eventType, call.phase(), call.callback());
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

    private ProxyObject createEventsEnumProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                try {
                    return Events.valueOf(key);
                } catch (IllegalArgumentException _) {
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
                } catch (IllegalArgumentException _) {
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

    private record Registration(Object eventTarget, EventPhase phase, Value callback) {
    }

    private record UnregisterFilters(EventPhase phase, Value callback) {
    }

    private record UnregisterCall(Object eventTarget, EventPhase phase, Value callback) {
    }
}
