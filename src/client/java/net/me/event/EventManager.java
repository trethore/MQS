package net.me.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.me.event.events.tick.EndClientTickEvent;
import net.me.event.events.tick.StartClientTickEvent;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
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
    private static final EventManager INSTANCE = new EventManager();

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

    private final Map<Class<? extends Event>, List<Listener>> listeners = new ConcurrentHashMap<>();

    private EventManager() {
    }

    public static EventManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> this.post(new StartClientTickEvent(client)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> this.post(new EndClientTickEvent(client)));
    }

    public void post(Event event) {
        List<Listener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Listener listener : eventListeners) {
                ScriptManager sm = ScriptManager.getInstance();
                sm.setCurrentScript(listener.owner());
                try {
                    listener.callback().execute(event);
                } catch (Exception e) {
                    LOGGER.error("Error executing event listener for {} in script '{}'",
                            event.getClass().getSimpleName(), listener.owner().getName(), e);
                } finally {
                    sm.clearCurrentScript();
                }
            }
        }
    }

    public void register(RunningScript owner, Class<? extends Event> eventType, Value callback) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new Listener(owner, callback));
    }

    public void unregister(RunningScript owner) {
        listeners.values().forEach(list -> list.removeIf(listener -> listener.owner().equals(owner)));
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
}