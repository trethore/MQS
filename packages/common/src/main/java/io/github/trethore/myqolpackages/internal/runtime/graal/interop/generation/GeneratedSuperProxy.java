/*
 * My QOL Packages - Client-side Minecraft modding at runtime
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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop.generation;

import io.github.trethore.myqolpackages.internal.runtime.graal.interop.JavaInteropAccess;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

final class GeneratedSuperProxy implements ProxyObject {
    private static final String EXPLICIT_FIELD_SUFFIX = "$";

    private final GeneratedTypeDefinition definition;
    private final Class<?> generatedClass;
    private final JavaInteropAccess interop;
    private final Object receiver;
    private final InvocationScope scope;
    private final Class<?> selectedInterface;

    GeneratedSuperProxy(
            GeneratedTypeDefinition definition,
            Class<?> generatedClass,
            JavaInteropAccess interop,
            Object receiver,
            Class<?> selectedInterface) {
        this(definition, generatedClass, interop, receiver, selectedInterface, new InvocationScope());
    }

    private GeneratedSuperProxy(
            GeneratedTypeDefinition definition,
            Class<?> generatedClass,
            JavaInteropAccess interop,
            Object receiver,
            Class<?> selectedInterface,
            InvocationScope scope) {
        this.definition = definition;
        this.generatedClass = generatedClass;
        this.interop = interop;
        this.receiver = receiver;
        this.selectedInterface = selectedInterface;
        this.scope = scope;
    }

    @Override
    public Object getMember(String key) {
        requireActive();
        if ("of".equals(key) && selectedInterface == null) {
            return (ProxyExecutable) this::selectInterface;
        }
        boolean explicitField = key.endsWith(EXPLICIT_FIELD_SUFFIX) && key.length() > 1;
        String exposedName = explicitField ? key.substring(0, key.length() - 1) : key;
        List<Method> methods = explicitField ? List.of() : getMethods(exposedName);
        if (!methods.isEmpty()) {
            return (ProxyExecutable) arguments -> invokeMethod(exposedName, methods, arguments);
        }
        Field field = getField(exposedName);
        return field == null ? null : readField(field);
    }

    @Override
    public Object getMemberKeys() {
        requireActive();
        Set<String> keys = new LinkedHashSet<>();
        if (selectedInterface == null) {
            keys.add("of");
        }
        Class<?> target = targetType();
        for (Method method : target.getMethods()) {
            if (isCallable(method)) {
                keys.add(method.getName());
            }
        }
        for (Field field : target.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                keys.add(field.getName());
                keys.add(field.getName() + EXPLICIT_FIELD_SUFFIX);
            }
        }
        return keys.toArray(String[]::new);
    }

    @Override
    public boolean hasMember(String key) {
        requireActive();
        if ("of".equals(key) && selectedInterface == null) {
            return true;
        }
        String exposedName = key.endsWith(EXPLICIT_FIELD_SUFFIX) ? key.substring(0, key.length() - 1) : key;
        return !getMethods(exposedName).isEmpty() || getField(exposedName) != null;
    }

    @Override
    public void putMember(String key, Value value) {
        requireActive();
        String exposedName = key.endsWith(EXPLICIT_FIELD_SUFFIX) ? key.substring(0, key.length() - 1) : key;
        Field field = getField(exposedName);
        if (field == null) {
            throw new UnsupportedOperationException("No accessible super field " + exposedName);
        }
        if (Modifier.isFinal(field.getModifiers())) {
            throw new UnsupportedOperationException("Cannot modify final super field " + exposedName);
        }
        Object converted = interop.convertValue(value, field.getType());
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(generatedClass, MethodHandles.lookup());
            MethodHandle setter = lookup.findSetter(field.getDeclaringClass(), field.getName(), field.getType());
            setter.invoke(receiver, converted);
        } catch (Throwable throwable) {
            throw invocationFailure("write super field " + exposedName, throwable);
        }
    }

    void invalidate() {
        scope.active = false;
    }

    private Object selectInterface(Value... arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("$super.of requires exactly one parent interface");
        }
        Class<?> interfaceType = interop.resolveClass(arguments[0]);
        if (!interfaceType.isInterface() || !definition.interfaces().contains(interfaceType)) {
            throw new IllegalArgumentException(interfaceType.getTypeName() + " is not a direct parent interface");
        }
        return new GeneratedSuperProxy(definition, generatedClass, interop, receiver, interfaceType, scope);
    }

    private Object invokeMethod(String exposedName, List<Method> methods, Value[] arguments) {
        requireActive();
        JavaInteropAccess.ResolvedMethod resolved = interop.resolveMethod(exposedName, methods, arguments);
        Method method = resolved.method();
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(generatedClass, MethodHandles.lookup());
            MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
            MethodHandle handle =
                    lookup.findSpecial(method.getDeclaringClass(), method.getName(), methodType, generatedClass);
            Object result = handle.bindTo(receiver).invokeWithArguments(resolved.arguments());
            return interop.wrapJavaValue(result);
        } catch (Throwable throwable) {
            throw invocationFailure("invoke super method " + exposedName, throwable);
        }
    }

    private Object readField(Field field) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(generatedClass, MethodHandles.lookup());
            MethodHandle getter = lookup.findGetter(field.getDeclaringClass(), field.getName(), field.getType());
            return interop.wrapJavaValue(getter.invoke(receiver));
        } catch (Throwable throwable) {
            throw invocationFailure("read super field " + field.getName(), throwable);
        }
    }

    private List<Method> getMethods(String exposedName) {
        if (selectedInterface != null) {
            return interop.getInstanceMethods(selectedInterface, exposedName).stream()
                    .filter(this::isCallable)
                    .toList();
        }
        List<Method> classMethods = interop.getInstanceMethods(definition.superclass(), exposedName).stream()
                .filter(this::isCallable)
                .toList();
        if (!classMethods.isEmpty()) {
            return classMethods;
        }
        List<Method> interfaceMethods = new ArrayList<>();
        for (Class<?> interfaceType : definition.interfaces()) {
            interop.getInstanceMethods(interfaceType, exposedName).stream()
                    .filter(this::isCallable)
                    .forEach(interfaceMethods::add);
        }
        return List.copyOf(interfaceMethods);
    }

    private Field getField(String exposedName) {
        Field field = interop.getInstanceField(targetType(), exposedName);
        return field != null && isFieldAccessible(field) ? field : null;
    }

    private Class<?> targetType() {
        return selectedInterface == null ? definition.superclass() : selectedInterface;
    }

    private boolean isCallable(Method method) {
        int modifiers = method.getModifiers();
        return !Modifier.isStatic(modifiers)
                && !Modifier.isPrivate(modifiers)
                && !Modifier.isAbstract(modifiers)
                && isMemberAccessible(method.getDeclaringClass(), modifiers);
    }

    private boolean isFieldAccessible(Field field) {
        int modifiers = field.getModifiers();
        return !Modifier.isStatic(modifiers) && isMemberAccessible(field.getDeclaringClass(), modifiers);
    }

    private boolean isMemberAccessible(Class<?> declaringClass, int modifiers) {
        if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
            return true;
        }
        return !Modifier.isPrivate(modifiers)
                && packageName(declaringClass.getName()).equals(packageName(generatedClass.getName()));
    }

    private void requireActive() {
        if (!scope.active) {
            throw new IllegalStateException("$super is no longer active");
        }
        if (Thread.currentThread() != scope.thread) {
            throw new IllegalStateException("$super cannot be used from another thread");
        }
    }

    private static String packageName(String binaryName) {
        int separator = binaryName.lastIndexOf('.');
        return separator < 0 ? "" : binaryName.substring(0, separator);
    }

    private static IllegalStateException invocationFailure(String operation, Throwable throwable) {
        return new IllegalStateException("Failed to " + operation + ": " + throwable, throwable);
    }

    private static final class InvocationScope {
        private final Thread thread = Thread.currentThread();
        private boolean active = true;
    }
}
