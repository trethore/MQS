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

import net.fabricmc.fabric.api.event.Event;
import net.me.mixin.fabric.event.ArrayBackedEventAccessor;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.minecraft.resources.Identifier;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class FabricEventAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricEventAdapter.class);

    private final ScriptManager scriptManager;
    private final Map<Event<?>, List<ScriptedFabricListener>> scriptedListeners = new ConcurrentHashMap<>();
    private final Map<Event<?>, Object> masterListeners = new ConcurrentHashMap<>();

    public FabricEventAdapter(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    private static Class<?> findListenerType(Event<?> fabricEvent) {
        if (fabricEvent instanceof ArrayBackedEventAccessor<?> accessor) {
            return accessor.getHandlers().getClass().getComponentType();
        }

        LOGGER.warn("Attempting to find listener type for non-ArrayBackedEvent: {}. This may fail.",
                fabricEvent.getClass());

        return Arrays.stream(fabricEvent.getClass().getMethods())
                .filter(m -> m.getName().equals("register")
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] != Identifier.class)
                .findFirst()
                .map(m -> m.getParameterTypes()[0])
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not find register method on event " + fabricEvent));
    }

    private static Method findSingleAbstractMethod(Class<?> listenerType) {
        return Arrays.stream(listenerType.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not find abstract method in " + listenerType.getName()));
    }

    private static Object getDefaultReturnValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        return switch (returnType.getName()) {
            case "boolean" -> false;
            case "byte" -> (byte) 0;
            case "short" -> (short) 0;
            case "int" -> 0;
            case "long" -> 0L;
            case "float" -> 0.0f;
            case "double" -> 0.0d;
            case "char" -> '\0';
            default -> null;
        };
    }

    public void register(RunningScript owner, Event<?> fabricEvent, Value jsCallback) {
        Object existingListener = masterListeners.get(fabricEvent);

        if (existingListener == null) {
            Object newListener = createMasterListenerProxy(fabricEvent);
            existingListener = masterListeners.putIfAbsent(fabricEvent, newListener);

            if (existingListener == null) {
                @SuppressWarnings({"rawtypes"})
                Event rawEvent = fabricEvent;
                //noinspection unchecked
                rawEvent.register(newListener);
                LOGGER.debug("Registered master listener for Fabric event: {}",
                        newListener.getClass().getInterfaces()[0].getName());
            }
        }

        scriptedListeners.computeIfAbsent(fabricEvent, ignored -> new CopyOnWriteArrayList<>())
                .add(new ScriptedFabricListener(owner, jsCallback));
    }

    public void unregister(RunningScript owner, Event<?> fabricEvent) {
        removeListeners(fabricEvent, listener -> listener.owner().equals(owner));
    }

    public void unregister(RunningScript owner, Event<?> fabricEvent, Value callback) {
        removeListeners(fabricEvent, listener ->
                listener.owner().equals(owner) && listener.jsCallback().equals(callback));
    }

    public void unregisterAll(RunningScript owner) {
        scriptedListeners.keySet().forEach(event -> unregister(owner, event));
    }

    private void removeListeners(Event<?> fabricEvent, Predicate<ScriptedFabricListener> filter) {
        List<ScriptedFabricListener> listeners = scriptedListeners.get(fabricEvent);
        if (listeners == null) {
            return;
        }
        listeners.removeIf(filter);
        if (listeners.isEmpty()) {
            scriptedListeners.remove(fabricEvent, listeners);
        }
    }

    private Object createMasterListenerProxy(Event<?> event) {
        Class<?> listenerType = findListenerType(event);
        Method eventMethod = findSingleAbstractMethod(listenerType);

        return Proxy.newProxyInstance(
                FabricEventAdapter.class.getClassLoader(),
                new Class<?>[]{listenerType},
                (proxy, method, args) -> dispatchProxyMethod(proxy, method, args, event, listenerType, eventMethod)
        );
    }

    private Object dispatchProxyMethod(Object proxy, Method method, Object[] args,
                                       Event<?> event, Class<?> listenerType, Method eventMethod) throws Throwable {
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }

        return switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> {
                if (method.equals(eventMethod)) {
                    executeScriptedListeners(event, listenerType, args);
                }
                yield getDefaultReturnValue(method.getReturnType());
            }
        };
    }

    private void executeScriptedListeners(Event<?> event, Class<?> listenerType, Object[] args) {
        List<ScriptedFabricListener> listeners = scriptedListeners.get(event);
        if (listeners == null) {
            return;
        }

        Object[] wrappedArgs = wrapArguments(args);
        for (ScriptedFabricListener listener : listeners) {
            invokeWithScriptContext(listener, listenerType, wrappedArgs);
        }
    }

    private Object[] wrapArguments(Object[] args) {
        return Arrays.stream(args)
                .map(arg -> ScriptUtils.wrapReturn(
                        arg,
                        scriptManager.getClassResolver().getMappingsManager(),
                        scriptManager))
                .toArray();
    }

    private void invokeWithScriptContext(ScriptedFabricListener listener, Class<?> listenerType, Object[] args) {
        RunningScript previousScript = scriptManager.getCurrentScript();
        scriptManager.setCurrentScript(listener.owner());

        try {
            listener.jsCallback().execute(args);
        } catch (Exception e) {
            LOGGER.error("Error executing Fabric event listener for {} in script '{}'",
                    listenerType.getSimpleName(), listener.owner().getName(), e);
        } finally {
            scriptManager.setCurrentScript(previousScript);
        }
    }

    private record ScriptedFabricListener(RunningScript owner, Value jsCallback) {
    }
}
