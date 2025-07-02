package net.me.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.event.events.tick.EndClientTickEvent;
import net.me.event.events.tick.StartClientTickEvent;
import net.me.mixin.fabric.event.ArrayBackedEventAccessor;
import net.me.mixin.fabric.event.EventPhaseDataAccessor;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.minecraft.util.Identifier;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);
    private final Map<Class<? extends Event>, List<Listener>> listeners = new ConcurrentHashMap<>();
    private final Map<RunningScript, List<FabricListener>> fabricListeners = new ConcurrentHashMap<>();
    private ScriptManager scriptManager;

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

    public void init(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        ClientTickEvents.START_CLIENT_TICK.register(client -> this.post(new StartClientTickEvent(client)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.post(new EndClientTickEvent(client)));

    }

    public void post(Event event) {
        List<Listener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Listener listener : eventListeners) {
                scriptManager.setCurrentScript(listener.owner());
                try {
                    listener.callback().execute(event);
                } catch (Exception e) {
                    LOGGER.error("Error executing event listener for {} in script '{}'",
                            event.getClass().getSimpleName(), listener.owner().getName(), e);
                } finally {
                    scriptManager.clearCurrentScript();
                }
            }
        }
    }

    public void register(RunningScript owner, Class<? extends Event> eventType, Value callback) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new Listener(owner, callback));
    }

    public void registerFabric(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent, Value jsCallback) {
        Class<?> listenerType = findListenerType(fabricEvent);
        Method sam = findSingleAbstractMethod(listenerType);

        Object listenerProxy = Proxy.newProxyInstance(
                EventManager.class.getClassLoader(),
                new Class<?>[]{listenerType},
                (proxy, method, args) -> {
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    if (method.equals(sam)) {
                        scriptManager.setCurrentScript(owner);
                        try {
                            Object[] wrappedArgs = new Object[args.length];
                            for (int i = 0; i < args.length; i++) {
                                wrappedArgs[i] = ScriptUtils.wrapReturn(args[i]);
                            }
                            jsCallback.execute(wrappedArgs);
                        } catch (Exception e) {
                            LOGGER.error("Error executing Fabric event listener for {} in script '{}'",
                                    listenerType.getSimpleName(), owner.getName(), e);
                        } finally {
                            scriptManager.clearCurrentScript();
                        }
                        return getReturnValueFor(sam.getReturnType());
                    }
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    return null;
                }
        );

        //noinspection unchecked,rawtypes
        ((net.fabricmc.fabric.api.event.Event) fabricEvent).register(listenerProxy);

        fabricListeners.computeIfAbsent(owner, k -> new CopyOnWriteArrayList<>())
                .add(new FabricListener(fabricEvent, listenerProxy, jsCallback));
    }

    public void unregister(RunningScript owner) {
        listeners.values().forEach(list -> list.removeIf(listener -> listener.owner().equals(owner)));

        List<FabricListener> ownedListeners = fabricListeners.remove(owner);
        if (ownedListeners != null) {
            for (FabricListener fl : ownedListeners) {
                unregisterFabricListener(fl.event(), fl.listenerProxy());
            }
        }
    }

    public void unregister(RunningScript owner, Class<? extends Event> eventType) {
        List<Listener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.removeIf(listener -> listener.owner().equals(owner));
        }
    }

    public void unregister(RunningScript owner, Class<? extends Event> eventType, Value callback) {
        List<Listener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(new Listener(owner, callback));
        }
    }

    public void unregister(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent) {
        List<FabricListener> ownedListeners = fabricListeners.get(owner);
        if (ownedListeners != null) {
            ownedListeners.removeIf(fl -> {
                if (fl.event().equals(fabricEvent)) {
                    unregisterFabricListener(fl.event(), fl.listenerProxy());
                    return true;
                }
                return false;
            });
        }
    }

    public void unregister(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent, Value callback) {
        List<FabricListener> ownedListeners = fabricListeners.get(owner);
        if (ownedListeners != null) {
            ownedListeners.removeIf(fl -> {
                if (fl.event().equals(fabricEvent) && fl.jsCallback().equals(callback)) {
                    unregisterFabricListener(fl.event(), fl.listenerProxy());
                    return true;
                }
                return false;
            });
        }
    }

    private void unregisterFabricListener(net.fabricmc.fabric.api.event.Event<?> event, Object listener) {
        if (!event.getClass().getName().equals("net.fabricmc.fabric.impl.base.event.ArrayBackedEvent")) {
            LOGGER.warn("Cannot unregister from an event that is not an ArrayBackedEvent: {}", event.getClass().getName());
            return;
        }

        try {
            ArrayBackedEventAccessor<?> accessor = (ArrayBackedEventAccessor<?>) event;
            synchronized (accessor.getLock()) {
                @SuppressWarnings("unchecked")
                Map<Identifier, EventPhaseDataAccessor<Object>> phases = (Map<Identifier, EventPhaseDataAccessor<Object>>) accessor.getPhases();
                EventPhaseDataAccessor<Object> phase = phases.get(net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);

                if (phase == null || phase.getListeners() == null) return;

                Object[] listeners = phase.getListeners();
                if (listeners.length == 0) return;

                int initialLength = listeners.length;
                List<Object> list = new ArrayList<>(Arrays.asList(listeners));

                if (list.remove(listener)) {
                    Object[] newListeners = list.toArray((Object[]) Array.newInstance(listeners.getClass().getComponentType(), 0));
                    phase.setListeners(newListeners);
                    accessor.callRebuildInvoker(initialLength - 1);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to unregister fabric listener", e);
        }
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

    private record FabricListener(net.fabricmc.fabric.api.event.Event<?> event, Object listenerProxy,
                                  Value jsCallback) {
    }
}