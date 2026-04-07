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
import net.me.scripting.script.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

public class FabricEventAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricEventAdapter.class);
    private static final Object[] EMPTY_ARGS = new Object[0];
    private static final String METHOD_EQUALS = "equals";
    private static final String METHOD_HASH_CODE = "hashCode";
    private static final String METHOD_TO_STRING = "toString";

    private final ScriptManager scriptManager;
    private final Map<Event<?>, List<ScriptedFabricListener>> scriptedListeners = new ConcurrentHashMap<>();
    private final Map<Event<?>, Object> masterListeners = new ConcurrentHashMap<>();
    private final Map<Event<?>, FabricEventBinding> eventBindings = new ConcurrentHashMap<>();

    public FabricEventAdapter(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
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
        FabricEventBinding binding = eventBindings.computeIfAbsent(fabricEvent, this::createBinding);
        Object existingListener = masterListeners.get(fabricEvent);

        if (existingListener == null) {
            Object newListener = createMasterListenerProxy(binding);
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

        List<ScriptedFabricListener> listeners = scriptedListeners.computeIfAbsent(fabricEvent, ignored -> new CopyOnWriteArrayList<>());
        listeners.add(new ScriptedFabricListener(owner, jsCallback));
        binding.rebuild(List.copyOf(listeners));
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

        FabricEventBinding binding = eventBindings.get(fabricEvent);
        if (listeners.isEmpty()) {
            scriptedListeners.remove(fabricEvent, listeners);
            if (binding != null) {
                binding.rebuild(List.of());
            }
            return;
        }

        if (binding != null) {
            binding.rebuild(List.copyOf(listeners));
        }
    }

    private FabricEventBinding createBinding(Event<?> event) {
        Class<?> listenerType = FabricEventIntrospection.findListenerType(event);
        Method eventMethod = FabricEventIntrospection.findSingleAbstractMethod(listenerType);

        if (!(event instanceof ArrayBackedEventAccessor<?>) && eventMethod.getReturnType() != void.class) {
            throw new IllegalArgumentException("Fabric event '" + event.getClass().getName()
                    + "' is not array-backed and returns a value, which MQS cannot safely proxy.");
        }

        FabricEventBinding binding = new FabricEventBinding(event, listenerType, eventMethod);
        binding.rebuild(List.of());
        return binding;
    }

    private Object createMasterListenerProxy(FabricEventBinding binding) {
        ClassLoader classLoader = binding.listenerType().getClassLoader();
        if (classLoader == null) {
            classLoader = FabricEventAdapter.class.getClassLoader();
        }

        return Proxy.newProxyInstance(
                classLoader,
                new Class<?>[]{binding.listenerType()},
                (proxy, method, args) -> dispatchProxyMethod(proxy, method, args, binding)
        );
    }

    private Object dispatchProxyMethod(Object proxy, Method method, Object[] args, FabricEventBinding binding) throws Throwable {
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }

        return switch (method.getName()) {
            case METHOD_EQUALS -> proxy == args[0];
            case METHOD_HASH_CODE -> System.identityHashCode(proxy);
            case METHOD_TO_STRING -> "MQS Fabric listener proxy for " + binding.listenerType().getName();
            default -> {
                if (method.equals(binding.eventMethod())) {
                    yield binding.invoke(args);
                }
                yield getDefaultReturnValue(method.getReturnType());
            }
        };
    }

    private static final class FabricEventInvocationException extends ReflectiveOperationException {
        private FabricEventInvocationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record ScriptedFabricListener(RunningScript owner, Value jsCallback) {
    }

    private final class FabricEventBinding {
        private final Event<?> event;
        private final Class<?> listenerType;
        private final Method eventMethod;
        private final AtomicReference<Object> invoker = new AtomicReference<>();

        private FabricEventBinding(Event<?> event, Class<?> listenerType, Method eventMethod) {
            this.event = event;
            this.listenerType = listenerType;
            this.eventMethod = eventMethod;
        }

        private Class<?> listenerType() {
            return listenerType;
        }

        private Method eventMethod() {
            return eventMethod;
        }

        private synchronized void rebuild(List<ScriptedFabricListener> listeners) {
            if (event instanceof ArrayBackedEventAccessor<?>) {
                invoker.set(createArrayBackedInvoker(listeners));
                return;
            }

            invoker.set(createVoidFallbackInvoker(listeners));
        }

        private Object invoke(Object[] args) throws FabricEventInvocationException {
            Object currentInvoker = invoker.get();
            try {
                return eventMethod.invoke(currentInvoker, args == null ? EMPTY_ARGS : args);
            } catch (IllegalAccessException exception) {
                throw new FabricEventInvocationException("Failed to invoke Fabric event listener method '" + eventMethod.getName() + "'.", exception);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new FabricEventInvocationException("Fabric event listener method '" + eventMethod.getName() + "' threw an exception.", cause);
            }
        }

        @SuppressWarnings("unchecked")
        private Object createArrayBackedInvoker(List<ScriptedFabricListener> listeners) {
            ArrayBackedEventAccessor<Object> accessor = (ArrayBackedEventAccessor<Object>) event;
            Function<Object[], Object> invokerFactory = accessor.getInvokerFactory();
            Object[] listenerArray = (Object[]) Array.newInstance(listenerType, listeners.size());

            for (int index = 0; index < listeners.size(); index++) {
                listenerArray[index] = createScriptListenerProxy(listeners.get(index));
            }

            return invokerFactory.apply(listenerArray);
        }

        private Object createScriptListenerProxy(ScriptedFabricListener listener) {
            ClassLoader classLoader = listenerType.getClassLoader();
            if (classLoader == null) {
                classLoader = FabricEventAdapter.class.getClassLoader();
            }

            return Proxy.newProxyInstance(
                    classLoader,
                    new Class<?>[]{listenerType},
                    (proxy, method, args) -> dispatchScriptListenerMethod(proxy, method, args, listener)
            );
        }

        private Object dispatchScriptListenerMethod(Object proxy, Method method, Object[] args,
                                                    ScriptedFabricListener listener) throws Throwable {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }

            return switch (method.getName()) {
                case METHOD_EQUALS -> proxy == args[0];
                case METHOD_HASH_CODE -> System.identityHashCode(proxy);
                case METHOD_TO_STRING -> "MQS scripted Fabric listener for " + listenerType.getName();
                default -> {
                    if (method.equals(eventMethod)) {
                        yield invokeWithScriptContext(listener, eventMethod.getReturnType(), args);
                    }
                    yield getDefaultReturnValue(method.getReturnType());
                }
            };
        }

        private Object invokeWithScriptContext(ScriptedFabricListener listener, Class<?> returnType, Object[] args) {
            RunningScript previousScript = scriptManager.getCurrentScript();
            scriptManager.setCurrentScript(listener.owner());

            try {
                Value result = listener.jsCallback().execute(wrapArguments(args));
                return convertReturnValue(result, returnType);
            } catch (Exception exception) {
                LOGGER.error("Error executing Fabric event listener for {} in script '{}'",
                        listenerType.getSimpleName(), listener.owner().getName(), exception);
                return getDefaultReturnValue(returnType);
            } finally {
                scriptManager.setCurrentScript(previousScript);
            }
        }

        private Object[] wrapArguments(Object[] args) {
            if (args == null || args.length == 0) {
                return EMPTY_ARGS;
            }

            return Arrays.stream(args)
                    .map(arg -> ScriptUtils.wrapReturn(
                            arg,
                            scriptManager.getClassResolver().getMappingsManager(),
                            scriptManager))
                    .toArray();
        }

        private Object convertReturnValue(Value result, Class<?> returnType) {
            if (returnType == void.class) {
                return null;
            }
            if (result == null || result.isNull()) {
                return getDefaultReturnValue(returnType);
            }

            Object unwrapped = ScriptUtils.unwrapReceiver(result);
            if (unwrapped != null && unwrapped != result) {
                Object primitiveValue = coercePrimitiveValue(returnType, unwrapped);
                if (primitiveValue != null) {
                    return primitiveValue;
                }
                if (returnType.isInstance(unwrapped)) {
                    return unwrapped;
                }
            }

            if (returnType.isEnum() && result.isString()) {
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) returnType.asSubclass(Enum.class), result.asString());
                    return enumValue;
                } catch (IllegalArgumentException ignored) {
                    LOGGER.warn("Invalid enum return value '{}' for Fabric listener return type {}", result.asString(), returnType.getName());
                    return getDefaultReturnValue(returnType);
                }
            }

            try {
                return result.as(returnType);
            } catch (Exception ignored) {
                return getDefaultReturnValue(returnType);
            }
        }

        private Object coercePrimitiveValue(Class<?> returnType, Object value) {
            if (returnType == boolean.class) {
                return value instanceof Boolean bool ? bool : null;
            }
            if (returnType == char.class) {
                return value instanceof Character character ? character : null;
            }
            if (!(value instanceof Number number)) {
                return null;
            }

            return switch (returnType.getName()) {
                case "byte" -> number.byteValue();
                case "short" -> number.shortValue();
                case "int" -> number.intValue();
                case "long" -> number.longValue();
                case "float" -> number.floatValue();
                case "double" -> number.doubleValue();
                default -> null;
            };
        }

        private Object createVoidFallbackInvoker(List<ScriptedFabricListener> listeners) {
            ClassLoader classLoader = listenerType.getClassLoader();
            if (classLoader == null) {
                classLoader = FabricEventAdapter.class.getClassLoader();
            }

            return Proxy.newProxyInstance(
                    classLoader,
                    new Class<?>[]{listenerType},
                    (proxy, method, args) -> {
                        if (method.isDefault()) {
                            return InvocationHandler.invokeDefault(proxy, method, args);
                        }

                        return switch (method.getName()) {
                            case METHOD_EQUALS -> proxy == args[0];
                            case METHOD_HASH_CODE -> System.identityHashCode(proxy);
                            case METHOD_TO_STRING -> "MQS Fabric fallback listener for " + listenerType.getName();
                            default -> {
                                if (method.equals(eventMethod)) {
                                    for (ScriptedFabricListener listener : listeners) {
                                        invokeWithScriptContext(listener, void.class, args);
                                    }
                                    yield null;
                                }
                                yield getDefaultReturnValue(method.getReturnType());
                            }
                        };
                    }
            );
        }
    }
}
