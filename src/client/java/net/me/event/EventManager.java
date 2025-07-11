package net.me.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.event.events.tick.EndClientTickEvent;
import net.me.event.events.tick.StartClientTickEvent;
import net.me.mixin.fabric.event.ArrayBackedEventAccessor;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.minecraft.util.Identifier;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);
    private final Map<Class<? extends Event>, Map<EventPhase, List<Listener>>> listeners = new ConcurrentHashMap<>();
    private final ScriptManager scriptManager;

    private final Map<net.fabricmc.fabric.api.event.Event<?>, List<ScriptedFabricListener>> scriptedFabricListeners = new ConcurrentHashMap<>();

    private final Map<net.fabricmc.fabric.api.event.Event<?>, Object> masterFabricListeners = new ConcurrentHashMap<>();

    public EventManager(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        ClientTickEvents.START_CLIENT_TICK.register(client -> this.post(new StartClientTickEvent(client)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.post(new EndClientTickEvent(client)));
    }

    private static Class<?> findListenerType(net.fabricmc.fabric.api.event.Event<?> fabricEvent) {
        if (!fabricEvent.getClass().getName().equals("net.fabricmc.fabric.impl.base.event.ArrayBackedEvent")) {
            LOGGER.warn("Attempting to find listener type for non-ArrayBackedEvent: {}. This may fail.", fabricEvent.getClass());
            return Arrays.stream(fabricEvent.getClass().getMethods())
                    .filter(m -> m.getName().equals("register") && m.getParameterCount() == 1 && m.getParameterTypes()[0] != Identifier.class)
                    .findFirst()
                    .map(m -> m.getParameterTypes()[0])
                    .orElseThrow(() -> new IllegalArgumentException("Could not find a single-argument register method on the event " + fabricEvent));
        }

        try {
            ArrayBackedEventAccessor<?> accessor = (ArrayBackedEventAccessor<?>) fabricEvent;
            Object[] handlers = accessor.getHandlers();
            return handlers.getClass().getComponentType();
        } catch (Exception e) {
            LOGGER.error("Failed to introspect Fabric event listener type via accessor", e);
            throw new IllegalStateException("Could not determine listener type for Fabric event: " + fabricEvent, e);
        }
    }

    private static Method findSingleAbstractMethod(Class<?> listenerType) {
        return Arrays.stream(listenerType.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find abstract method in " + listenerType.getName()));
    }

    private static Object getReturnValueFor(Class<?> clazz) {
        if (clazz == boolean.class) return false;
        if (clazz.isPrimitive()) return 0;
        return null;
    }

    public void post(Event event) {
        Map<EventPhase, List<Listener>> phaseListeners = listeners.get(event.getClass());
        if (phaseListeners == null || phaseListeners.isEmpty()) {
            return;
        }

        List<Listener> preListeners = phaseListeners.get(EventPhase.PRE);
        if (preListeners != null) {
            for (Listener listener : preListeners) {
                RunningScript previousScript = scriptManager.getCurrentScript();
                scriptManager.setCurrentScript(listener.owner());
                try {
                    listener.callback().execute(event);
                } catch (Exception e) {
                    LOGGER.error("Error executing event listener for {} in script '{}' during phase {}",
                            event.getClass().getSimpleName(), listener.owner().getName(), EventPhase.PRE, e);
                } finally {
                    scriptManager.setCurrentScript(previousScript);
                }
            }
        }

        if (event instanceof CancellableEvent && ((CancellableEvent) event).isCancelled()) {
            return;
        }

        List<Listener> postListeners = phaseListeners.get(EventPhase.POST);
        if (postListeners != null) {
            for (Listener listener : postListeners) {
                RunningScript previousScript = scriptManager.getCurrentScript();
                scriptManager.setCurrentScript(listener.owner());
                try {
                    listener.callback().execute(event);
                } catch (Exception e) {
                    LOGGER.error("Error executing event listener for {} in script '{}' during phase {}",
                            event.getClass().getSimpleName(), listener.owner().getName(), EventPhase.POST, e);
                } finally {
                    scriptManager.setCurrentScript(previousScript);
                }
            }
        }
    }

    public void register(RunningScript owner, Class<? extends Event> eventType, Value callback) {
        register(owner, eventType, EventPhase.POST, callback);
    }

    public void register(RunningScript owner, Class<? extends Event> eventType, EventPhase phase, Value callback) {
        listeners.computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(phase, k -> new CopyOnWriteArrayList<>())
                .add(new Listener(owner, callback));
    }

    public void register(RunningScript owner, Events eventEnum, Value callback) {
        register(owner, eventEnum.getEventClass(), EventPhase.POST, callback);
    }

    public void register(RunningScript owner, Events eventEnum, EventPhase phase, Value callback) {
        register(owner, eventEnum.getEventClass(), phase, callback);
    }

    public void registerFabric(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent, Value jsCallback) {
        masterFabricListeners.computeIfAbsent(fabricEvent, event -> {
            Class<?> listenerType = findListenerType(event);
            Method sam = findSingleAbstractMethod(listenerType);

            Object masterListenerProxy = Proxy.newProxyInstance(
                    EventManager.class.getClassLoader(),
                    new Class<?>[]{listenerType},
                    (proxy, method, args) -> {
                        if (method.isDefault()) {
                            return InvocationHandler.invokeDefault(proxy, method, args);
                        }
                        if (method.equals(sam)) {
                            List<ScriptedFabricListener> listeners = scriptedFabricListeners.get(event);
                            if (listeners != null) {
                                for (ScriptedFabricListener scriptedListener : listeners) {
                                    RunningScript previousScript = scriptManager.getCurrentScript();
                                    scriptManager.setCurrentScript(scriptedListener.owner());
                                    try {
                                        Object[] wrappedArgs = new Object[args.length];
                                        for (int i = 0; i < args.length; i++) {
                                            wrappedArgs[i] = ScriptUtils.wrapReturn(args[i]);
                                        }
                                        scriptedListener.jsCallback().execute(wrappedArgs);
                                    } catch (Exception e) {
                                        LOGGER.error("Error executing Fabric event listener for {} in script '{}'",
                                                listenerType.getSimpleName(), scriptedListener.owner().getName(), e);
                                    } finally {
                                        scriptManager.setCurrentScript(previousScript);
                                    }
                                }
                            }
                            return getReturnValueFor(sam.getReturnType());
                        }
                        if (method.getName().equals("equals")) return proxy == args[0];
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        return null;
                    }
            );

            //noinspection unchecked,rawtypes
            ((net.fabricmc.fabric.api.event.Event) event).register(masterListenerProxy);
            LOGGER.debug("Registered master listener for Fabric event: {}", listenerType.getName());
            return masterListenerProxy;
        });

        scriptedFabricListeners.computeIfAbsent(fabricEvent, k -> new CopyOnWriteArrayList<>())
                .add(new ScriptedFabricListener(owner, jsCallback));
    }

    public void unregisterAll(RunningScript owner) {
        listeners.values().forEach(phaseMap ->
                phaseMap.values().forEach(list ->
                        list.removeIf(listener -> listener.owner().equals(owner))
                )
        );

        scriptedFabricListeners.values().forEach(list ->
                list.removeIf(listener -> listener.owner().equals(owner))
        );
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
        List<ScriptedFabricListener> listeners = scriptedFabricListeners.get(fabricEvent);
        if (listeners != null) {
            listeners.removeIf(listener -> listener.owner().equals(owner));
        }
    }

    public void unregister(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent, Value callback) {
        List<ScriptedFabricListener> listeners = scriptedFabricListeners.get(fabricEvent);
        if (listeners != null) {
            listeners.removeIf(listener -> listener.owner().equals(owner) && listener.jsCallback().equals(callback));
        }
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

    private record ScriptedFabricListener(RunningScript owner, Value jsCallback) {
    }
}