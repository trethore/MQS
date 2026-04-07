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

import net.me.event.*;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.internal.HandleTracker;
import net.me.scripting.api.internal.ScriptContextHelper;
import net.me.scripting.script.RunningScript;
import net.me.scripting.typings.FabricEventTypeDescriptors;
import net.me.scripting.typings.MqsApiFragment;
import net.me.scripting.typings.TypingsConstants;
import net.me.scripting.typings.schema.TsMember;
import net.me.scripting.typings.schema.TsObject;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.me.scripting.typings.schema.TsDescriptors.*;

public class EventsAPI implements ProxyObject {
    private static final String API_NAME = "Events API";
    private static final String REGISTER = "register";
    private static final String UNREGISTER = "unregister";
    private static final String UNREGISTER_ALL = "unregisterAll";
    private static final String EVENTS = "Events";
    private static final String PHASE = "Phase";
    private static final String FABRIC = "fabric";
    private static final String OFF = "off";
    private static final String OPTIONS = "options";
    private static final String TARGET = "target";
    private static final String EVENT_TYPE = "eventType";
    private static final String EVENT_OPTIONS_LIKE = "MQSEventOptionsLike";
    private static final String EVENT_PHASE = "MQSEventPhase";
    private static final String EVENT_ENUM = "MQSEventsEnum";
    private static final String EVENT_PHASE_VALUES = "MQSEventPhaseValues";
    private static final String EVENT_SUBSCRIPTION_OPTIONS = "MQSEventSubscriptionOptions";
    private static final String EVENTS_API = "MQSEventsApi";
    private static final String CALLBACK_MUST_BE_FUNCTION = "Callback must be a function.";
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
    private final Map<String, Events> namedEvents = new LinkedHashMap<>();
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

    public static MqsApiFragment describeTypeScript() {
        return MqsApiFragment.merge(
                new MqsApiFragment(
                        List.of(
                                alias(EVENT_PHASE, "\"PRE\" | \"POST\""),
                                alias(TypingsConstants.EVENT_CALLBACK, "<T = any>", "(event: T) => " + TypingsConstants.UNKNOWN),
                                alias(EVENT_OPTIONS_LIKE, EVENT_SUBSCRIPTION_OPTIONS + " | " + TypingsConstants.EVENT_OPTIONS_BUILDER + " | " + EVENT_PHASE + " | " + TypingsConstants.STRING)
                        ),
                        List.of(),
                        List.of(),
                        List.of(
                                describePhaseValues(),
                                describeEventsEnum(),
                                describeEventSubscriptionOptions(),
                                describeEventOptionsBuilder(),
                                describeEventsApi()
                        )
                ),
                FabricEventTypeDescriptors.describeTypeScript()
        );
    }

    private static TsObject describePhaseValues() {
        return new TsObject(
                EVENT_PHASE_VALUES,
                List.of(
                        ro("PRE", "\"PRE\""),
                        ro("POST", "\"POST\"")
                )
        );
    }

    private static TsObject describeEventsEnum() {
        List<TsMember> members = new ArrayList<>();
        for (Events event : Events.values()) {
            members.add(ro(event.name(), "\"" + event.name() + "\""));
        }
        return new TsObject(EVENT_ENUM, List.copyOf(members));
    }

    private static TsObject describeEventSubscriptionOptions() {
        return new TsObject(
                EVENT_SUBSCRIPTION_OPTIONS,
                List.of(prop(TypingsConstants.PHASE, EVENT_PHASE))
        );
    }

    private static TsObject describeEventOptionsBuilder() {
        return new TsObject(
                TypingsConstants.EVENT_OPTIONS_BUILDER,
                List.of(
                        method(TypingsConstants.PHASE, fn(TypingsConstants.EVENT_OPTIONS_BUILDER, p(TypingsConstants.PHASE, EVENT_PHASE + " | " + TypingsConstants.STRING))),
                        method("build", fn(EVENT_SUBSCRIPTION_OPTIONS))
                )
        );
    }

    private static TsObject describeEventsApi() {
        List<TsMember> members = new ArrayList<>(List.of(
                ro(FABRIC, FabricEventTypeDescriptors.ROOT_OBJECT_NAME),
                ro(EVENTS, EVENT_ENUM),
                ro(PHASE, EVENT_PHASE_VALUES),
                method(OPTIONS, fn(TypingsConstants.EVENT_OPTIONS_BUILDER)),
                method(OFF, fn(TypingsConstants.VOID, opt(TARGET, TypingsConstants.MQS_DISPOSER + " | " + TypingsConstants.EVENT_CALLBACK))),
                method(
                        REGISTER,
                        fn("<TEvent extends " + FabricEventTypeDescriptors.EVENT_TYPE_BOUND + ">",
                                TypingsConstants.MQS_DISPOSER,
                                p(EVENT_TYPE, "TEvent"),
                                p(TypingsConstants.CALLBACK, FabricEventTypeDescriptors.EVENT_CALLBACK_TYPE_NAME + "<TEvent>")),
                        fn(TypingsConstants.MQS_DISPOSER, p(EVENT_TYPE, TypingsConstants.UNKNOWN), p(TypingsConstants.CALLBACK, TypingsConstants.EVENT_CALLBACK)),
                        fn(TypingsConstants.MQS_DISPOSER, p(EVENT_TYPE, TypingsConstants.UNKNOWN), p(TypingsConstants.PHASE, EVENT_PHASE + " | " + TypingsConstants.STRING), p(TypingsConstants.CALLBACK, TypingsConstants.EVENT_CALLBACK))
                ),
                method(UNREGISTER, fn(TypingsConstants.VOID, p("eventTarget", TypingsConstants.UNKNOWN), opt("phaseOrCallback", TypingsConstants.UNKNOWN), opt(TypingsConstants.CALLBACK, TypingsConstants.EVENT_CALLBACK))),
                method(UNREGISTER_ALL, fn(TypingsConstants.VOID))
        ));

        for (Events event : Events.values()) {
            members.add(method(
                    buildHandlerName(event),
                    fn(TypingsConstants.MQS_DISPOSER, p(TypingsConstants.CALLBACK, TypingsConstants.EVENT_CALLBACK), opt(OPTIONS, EVENT_OPTIONS_LIKE))
            ));
        }

        return new TsObject(EVENTS_API, List.copyOf(members));
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

    private static String buildHandlerName(Events event) {
        String[] parts = event.name().split("_");
        StringBuilder builder = new StringBuilder("on");

        for (String part : parts) {
            if (!part.isEmpty() && !"EVENT".equals(part)) {
                String normalizedPart = part.toLowerCase(Locale.ROOT);
                builder.append(Character.toUpperCase(normalizedPart.charAt(0)));
                if (normalizedPart.length() > 1) {
                    builder.append(normalizedPart, 1, normalizedPart.length());
                }
            }
        }

        return builder.toString();
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case FABRIC -> fabricProxy;
            case OFF -> createOffExecutable();
            case OPTIONS -> createOptionsExecutable();
            case EVENTS -> eventsEnumProxy;
            case PHASE -> eventPhaseEnumProxy;
            case REGISTER -> (ProxyExecutable) args -> register(requireOwner(), args);
            case UNREGISTER -> (ProxyExecutable) args -> unregister(requireOwner(), args);
            case UNREGISTER_ALL -> (ProxyExecutable) args -> unregisterAll(requireOwner(), args);
            default -> resolveNamedEventMember(key);
        };
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

    private Object resolveNamedEventMember(String key) {
        Events mappedEvent = namedEvents.get(key);
        if (mappedEvent == null) {
            return null;
        }
        return createEventExecutable(mappedEvent);
    }

    private RunningScript requireOwner() {
        return contextHelper.require(API_NAME);
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
        return new FabricEventRegistry().createProxy();
    }

    private ProxyExecutable createOffExecutable() {
        return args -> {
            RunningScript owner = requireOwner();
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
        return ignored -> requireOwner().getContext().asValue(EventSubscriptionOptions.builder());
    }

    private ProxyExecutable createEventExecutable(Events mappedEvent) {
        return args -> {
            RunningScript owner = requireOwner();
            Value callback = ApiArgumentChecks.requireExecutable(args, 0, "First argument must be a callback function.");
            EventSubscriptionOptions options = EventSubscriptionOptions.fromScript(args.length > 1 ? args[1] : null, EventPhase.POST);
            EventPhase phase = options.phase();
            eventManager.register(owner, mappedEvent, phase, callback);
            return registerHandle(owner, mappedEvent, mappedEvent.getEventClass(), phase, callback, null);
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
            Value callback = ApiArgumentChecks.requireExecutable(args, 1, CALLBACK_MUST_BE_FUNCTION);
            return new Registration(eventTarget, EventPhase.POST, callback);
        }
        if (eventTarget instanceof net.fabricmc.fabric.api.event.Event<?>) {
            throw new IllegalArgumentException("Fabric events do not support phases.");
        }
        EventPhase phase = resolvePhase(args[1]);
        if (phase == null) {
            throw new IllegalArgumentException("Second argument must be a valid phase (PRE or POST).");
        }
        Value callback = ApiArgumentChecks.requireExecutable(args, 2, CALLBACK_MUST_BE_FUNCTION);
        return new Registration(eventTarget, phase, callback);
    }

    private Value registerTarget(RunningScript owner, Registration registration) {
        switch (registration.eventTarget()) {
            case Events eventEnum -> {
                eventManager.register(owner, eventEnum, registration.phase(), registration.callback());
                return registerHandle(owner, eventEnum, eventEnum.getEventClass(), registration.phase(), registration.callback(), null);
            }
            case Class<?> cls when Event.class.isAssignableFrom(cls) -> {
                //noinspection unchecked
                Class<? extends Event> eventClass = (Class<? extends Event>) cls;
                eventManager.register(owner, eventClass, registration.phase(), registration.callback());
                return registerHandle(owner, null, eventClass, registration.phase(), registration.callback(), null);
            }
            case net.fabricmc.fabric.api.event.Event<?> fabricEvent -> {
                eventManager.registerFabric(owner, fabricEvent, registration.callback());
                return registerHandle(owner, null, null, null, registration.callback(), fabricEvent);
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

    private Value registerHandle(RunningScript owner,
                                 Events event,
                                 Class<? extends Event> eventClass,
                                 EventPhase phase,
                                 Value callback,
                                 net.fabricmc.fabric.api.event.Event<?> fabricEvent) {
        EventHandle handle = new EventHandle(owner, event, eventClass, phase, callback, fabricEvent);
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

    private record Registration(Object eventTarget, EventPhase phase, Value callback) {
    }

    private record UnregisterFilters(EventPhase phase, Value callback) {
    }

    private record UnregisterCall(Object eventTarget, EventPhase phase, Value callback) {
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

            unregister();
        }

        private void unregister() {
            if (fabricEvent != null) {
                eventManager.unregister(owner, fabricEvent, callback);
                return;
            }

            if (eventClass != null) {
                unregisterEventClass();
                return;
            }

            if (event != null) {
                unregisterEvent();
            }
        }

        private void unregisterEventClass() {
            if (phase == null) {
                eventManager.unregister(owner, eventClass, callback);
                return;
            }

            eventManager.unregister(owner, eventClass, phase, callback);
        }

        private void unregisterEvent() {
            if (phase == null) {
                eventManager.unregister(owner, event, callback);
                return;
            }

            eventManager.unregister(owner, event, phase, callback);
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
            return a.isProxyObject() && b.isProxyObject() && a.asProxyObject() == b.asProxyObject();
        }
    }
}
