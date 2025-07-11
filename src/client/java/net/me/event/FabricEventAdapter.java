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

package net.me.event;

import net.me.mixin.fabric.event.ArrayBackedEventAccessor;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
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

public class FabricEventAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricEventAdapter.class);

    private final ScriptManager scriptManager;
    private final Map<net.fabricmc.fabric.api.event.Event<?>, List<ScriptedFabricListener>> scriptedFabricListeners = new ConcurrentHashMap<>();
    private final Map<net.fabricmc.fabric.api.event.Event<?>, Object> masterFabricListeners = new ConcurrentHashMap<>();

    public FabricEventAdapter(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    private static Class<?> findListenerType(net.fabricmc.fabric.api.event.Event<?> fabricEvent) {
        if (!fabricEvent.getClass().getName().equals("net.fabricmc.fabric.impl.base.event.ArrayBackedEvent")) {
            LOGGER.warn("Attempting to find listener type for non-ArrayBackedEvent: {}. This may fail.", fabricEvent.getClass());
            return Arrays.stream(fabricEvent.getClass().getMethods())
                    .filter(m -> m.getName().equals("register") && m.getParameterCount() == 1 && m.getParameterTypes()[0] != net.minecraft.util.Identifier.class)
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

    public void register(RunningScript owner, net.fabricmc.fabric.api.event.Event<?> fabricEvent, Value jsCallback) {
        Object masterListener = masterFabricListeners.computeIfAbsent(fabricEvent, this::createMasterListenerProxy);

        if (masterFabricListeners.get(fabricEvent) == masterListener) {
            //noinspection unchecked,rawtypes
            ((net.fabricmc.fabric.api.event.Event) fabricEvent).register(masterListener);
            Class<?> listenerInterface = masterListener.getClass().getInterfaces()[0];
            LOGGER.debug("Registered new master listener for Fabric event: {}", listenerInterface.getName());
        }

        scriptedFabricListeners.computeIfAbsent(fabricEvent, k -> new CopyOnWriteArrayList<>())
                .add(new ScriptedFabricListener(owner, jsCallback));
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

    public void unregisterAll(RunningScript owner) {
        scriptedFabricListeners.values().forEach(list ->
                list.removeIf(listener -> listener.owner().equals(owner))
        );
    }

    private Object createMasterListenerProxy(net.fabricmc.fabric.api.event.Event<?> event) {
        Class<?> listenerType = findListenerType(event);
        Method singleAbstractMethod = findSingleAbstractMethod(listenerType);

        return Proxy.newProxyInstance(
                FabricEventAdapter.class.getClassLoader(),
                new Class<?>[]{listenerType},
                (proxy, method, args) -> {
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    if (method.equals(singleAbstractMethod)) {
                        executeScriptedListeners(event, listenerType, args);
                        return getReturnValueFor(singleAbstractMethod.getReturnType());
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    return null;
                }
        );
    }

    private void executeScriptedListeners(net.fabricmc.fabric.api.event.Event<?> event, Class<?> listenerType, Object[] args) {
        List<ScriptedFabricListener> listeners = scriptedFabricListeners.get(event);
        if (listeners == null) {
            return;
        }

        for (ScriptedFabricListener scriptedListener : listeners) {
            RunningScript previousScript = scriptManager.getCurrentScript();
            scriptManager.setCurrentScript(scriptedListener.owner());
            try {
                Object[] wrappedArgs = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    wrappedArgs[i] = ScriptUtils.wrapReturn(
                            args[i],
                            scriptManager.getClassResolver().getMappingsManager(),
                            scriptManager
                    );
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

    private record ScriptedFabricListener(RunningScript owner, Value jsCallback) {
    }
}