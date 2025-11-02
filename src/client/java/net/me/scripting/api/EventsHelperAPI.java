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
import net.me.event.Events;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EventsHelperAPI implements ProxyObject {
    private static final Set<String> MEMBER_KEYS = Set.of("fabric");

    private final EventManager eventManager;
    private final ScriptManager scriptManager;
    private final Map<String, Events> namedEvents = new HashMap<>();
    private final ProxyObject fabricProxy;

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
        if ("fabric".equals(key)) {
            return fabricProxy;
        }
        Events mappedEvent = namedEvents.get(key);
        if (mappedEvent == null) {
            return null;
        }
        return (ProxyExecutable) args -> {
            RunningScript owner = getCurrentScript();
            Value callback = args.length > 0 ? args[0] : null;
            if (callback == null || !callback.canExecute()) {
                throw new IllegalArgumentException("First argument must be a callback function.");
            }

            EventPhase phase = EventPhase.POST;
            if (args.length > 1 && args[1] != null && args[1].hasMembers() && args[1].hasMember("phase")) {
                phase = resolvePhase(args[1].getMember("phase"));
            }

            eventManager.register(owner, mappedEvent, phase, callback);
            return null;
        };
    }

    @Override
    public Object getMemberKeys() {
        var keys = new String[namedEvents.size() + MEMBER_KEYS.size()];
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
                    Value callback = args.length > 0 ? args[0] : null;
                    if (callback == null || !callback.canExecute()) {
                        throw new IllegalArgumentException("First argument must be a callback function.");
                    }
                    eventManager.registerFabric(owner, fabricEvent, callback);
                    return null;
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

    private EventPhase resolvePhase(Value phaseValue) {
        if (phaseValue == null) {
            return EventPhase.POST;
        }
        if (phaseValue.isHostObject() && phaseValue.asHostObject() instanceof EventPhase eventPhase) {
            return eventPhase;
        }
        if (phaseValue.isString()) {
            String text = phaseValue.asString();
            if (text != null) {
                try {
                    return EventPhase.valueOf(text.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid phase '" + text + "'. Use PRE or POST.");
                }
            }
        }
        throw new IllegalArgumentException("Phase must be a string ('PRE'|'POST') or an EventPhase value.");
    }

    private String buildHandlerName(Events event) {
        String rawName = event.name();
        int idx = rawName.lastIndexOf("Event");
        if (idx >= 0) {
            rawName = rawName.substring(0, idx) + rawName.substring(idx + "Event".length());
        }
        return "on" + rawName;
    }
}
