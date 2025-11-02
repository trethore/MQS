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

import net.me.event.Event;
import net.me.event.EventManager;
import net.me.event.EventPhase;
import net.me.event.Events;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Arrays;
import java.util.Locale;
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
        Class<? extends Event> eventType;
        if (eventTarget instanceof Events eventEnum) {
            eventType = eventEnum.getEventClass();
        } else if (eventTarget instanceof Class<?> cls && Event.class.isAssignableFrom(cls)) {
            //noinspection unchecked
            eventType = (Class<? extends Event>) cls;
        } else {
            throw new IllegalArgumentException("First argument to EventManager.unregister must be an MQS event class, a Fabric Event, or an MQS Event from EventManager.Events.");
        }
        return eventType;
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

        return (ProxyExecutable) args -> {
            RunningScript owner = getCurrentScript();

            if (REGISTER.equals(key)) {
                if (args.length < 2 || args.length > 3) {
                    throw new IllegalArgumentException("Usage: EventManager.register(EventType, [Phase], callbackFunction)");
                }

                Object eventTarget;
                EventPhase phase;
                Value callback;

                if (args.length == 2) {
                    eventTarget = resolveEventTarget(args[0]);
                    phase = EventPhase.POST;
                    callback = args[1];
                    if (!callback.canExecute()) {
                        throw new IllegalArgumentException("Callback must be a function.");
                    }
                } else {
                    eventTarget = resolveEventTarget(args[0]);
                    phase = resolvePhase(args[1]);
                    if (phase == null) {
                        throw new IllegalArgumentException("Second argument must be a valid phase (PRE or POST).");
                    }
                    callback = args[2];
                    if (!callback.canExecute()) {
                        throw new IllegalArgumentException("Callback must be a function.");
                    }
                }

                switch (eventTarget) {
                    case Events eventEnum -> eventManager.register(owner, eventEnum, phase, callback);
                    case Class<?> cls when Event.class.isAssignableFrom(cls) ->
                        //noinspection unchecked
                            eventManager.register(owner, (Class<? extends Event>) cls, phase, callback);
                    case net.fabricmc.fabric.api.event.Event<?> fabricEvent ->
                            eventManager.registerFabric(owner, fabricEvent, callback);
                    case null, default ->
                            throw new IllegalArgumentException("First argument to EventManager.register must be a MQS event class, a Fabric Event object, or an MQS Event from EventManager.Events.");
                }
                return null;
            }

            if (UNREGISTER_ALL.equals(key)) {
                if (args.length != 0) {
                    throw new IllegalArgumentException("Usage: EventManager.unregisterAll()");
                }
                eventManager.unregisterAll(owner);
                return null;
            }

            if (UNREGISTER.equals(key)) {
                if (args.length < 1 || args.length > 3) {
                    throw new IllegalArgumentException("Usage: EventManager.unregister(EventType, [Phase|callback], [callback])");
                }

                Object eventTarget = resolveEventTarget(args[0]);
                EventPhase phase = null;
                Value callback = null;

                if (args.length > 1) {
                    EventPhase potentialPhase = resolvePhase(args[1]);
                    if (potentialPhase != null) {
                        phase = potentialPhase;
                        if (args.length > 2) {
                            if (args[2].canExecute()) {
                                callback = args[2];
                            } else {
                                throw new IllegalArgumentException("Third argument must be a callback function.");
                            }
                        }
                    } else if (args[1].canExecute()) {
                        callback = args[1];
                        if (args.length > 2) {
                            throw new IllegalArgumentException("Cannot provide a third argument when the second is a callback.");
                        }
                    } else {
                        throw new IllegalArgumentException("Second argument must be a Phase or a callback function.");
                    }
                }

                if (eventTarget instanceof net.fabricmc.fabric.api.event.Event<?> fabricEvent) {
                    if (phase != null) {
                        throw new IllegalArgumentException("Fabric events do not support phases.");
                    }
                    if (callback != null) {
                        eventManager.unregister(owner, fabricEvent, callback);
                    } else {
                        eventManager.unregister(owner, fabricEvent);
                    }
                } else {
                    Class<? extends Event> eventType = getEventClass(eventTarget);

                    if (phase != null && callback != null) {
                        eventManager.unregister(owner, eventType, phase, callback);
                    } else if (phase != null) {
                        eventManager.unregister(owner, eventType, phase);
                    } else if (callback != null) {
                        eventManager.unregister(owner, eventType, callback);
                    } else {
                        eventManager.unregister(owner, eventType);
                    }
                }
                return null;
            }

            throw new UnsupportedOperationException("Unsupported EventManager operation: " + key);
        };
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
        if (phaseValue == null) {
            return null;
        }
        if (phaseValue.isHostObject()) {
            Object hostObject = phaseValue.asHostObject();
            if (hostObject instanceof EventPhase eventPhase) {
                return eventPhase;
            }
        }
        if (phaseValue.isString()) {
            String phaseName = phaseValue.asString();
            if (phaseName != null) {
                try {
                    return EventPhase.valueOf(phaseName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid phase '" + phaseName + "'. Use PRE or POST.");
                }
            }
        }
        return null;
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

    private ProxyObject createEventsEnumProxy() {
        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                try {
                    return Events.valueOf(key);
                } catch (IllegalArgumentException e) {
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
                } catch (IllegalArgumentException e) {
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
}
