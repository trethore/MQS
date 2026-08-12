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
import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.jar.asm.Type;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

final class GeneratedCallbackBinding {
    private final List<Object> fieldValues;
    private final Map<String, GeneratedTypeDefinition.ConstructorDefinition> constructors = new HashMap<>();
    private final GeneratedTypeDefinition definition;
    private final JavaInteropAccess interop;
    private final Map<String, GeneratedTypeDefinition.MethodDefinition> methods = new HashMap<>();
    private final JavaTypeGenerationSession session;

    private volatile boolean active = true;
    private Class<?> generatedClass;

    GeneratedCallbackBinding(
            GeneratedTypeDefinition definition, JavaInteropAccess interop, JavaTypeGenerationSession session) {
        this.definition = definition;
        this.interop = interop;
        this.session = session;
        for (GeneratedTypeDefinition.MethodDefinition method : definition.methods()) {
            methods.put(method.runtimeName() + methodDescriptor(method), method);
        }
        for (GeneratedTypeDefinition.ConstructorDefinition constructor : definition.constructors()) {
            constructors.put(constructorDescriptor(constructor.argumentTypes()), constructor);
        }
        this.fieldValues =
                definition.fields().stream().map(this::convertFieldValue).toList();
    }

    void bindClass(Class<?> generatedClass) {
        this.generatedClass = generatedClass;
    }

    Class<?> generatedClass() {
        return generatedClass;
    }

    Object invoke(Method method, Object receiver, Object[] arguments) {
        return session.execute(() -> {
            requireActive();
            GeneratedTypeDefinition.MethodDefinition methodDefinition =
                    methods.get(method.getName() + Type.getMethodDescriptor(method));
            if (methodDefinition == null || methodDefinition.implementation() == null) {
                throw new IllegalStateException("No generated callback for " + method.toGenericString());
            }
            GeneratedSuperProxy superProxy = null;
            try {
                Object[] callbackArguments;
                if (methodDefinition.isStatic()) {
                    callbackArguments = wrapArguments(arguments, 0);
                } else {
                    superProxy = new GeneratedSuperProxy(definition, generatedClass, interop, receiver, null);
                    callbackArguments = wrapArguments(arguments, 2);
                    callbackArguments[0] = interop.wrapJavaValue(receiver, definition.binaryName());
                    callbackArguments[1] = superProxy;
                }
                Value result = methodDefinition.implementation().execute(callbackArguments);
                return methodDefinition.returnType() == Void.TYPE
                        ? null
                        : interop.convertValue(result, methodDefinition.returnType());
            } catch (RuntimeException exception) {
                throw callbackFailure("method " + methodDefinition.exposedName(), exception);
            } finally {
                if (superProxy != null) {
                    superProxy.invalidate();
                }
            }
        });
    }

    GeneratedConstructorSelection selectConstructor(String descriptor, Object[] arguments) {
        return session.execute(() -> {
            requireActive();
            GeneratedTypeDefinition.ConstructorDefinition constructor = constructors.get(descriptor);
            if (constructor == null) {
                throw new IllegalStateException("No generated constructor callback for " + descriptor);
            }
            SuperRecorder recorder = new SuperRecorder();
            Object[] callbackArguments = wrapArguments(arguments, 2);
            callbackArguments[0] = NoSelfProxy.INSTANCE;
            callbackArguments[1] = recorder;
            try {
                constructor.implementation().execute(callbackArguments);
            } catch (PolyglotException exception) {
                if (!exception.isHostException() || !(exception.asHostException() instanceof SuperSelectionSignal)) {
                    throw callbackFailure("constructor selector " + descriptor, exception);
                }
            }
            Value[] superArguments = recorder.arguments;
            if (superArguments == null) {
                throw new IllegalStateException("Constructor did not call $super(...)");
            }
            List<Constructor<?>> candidates = GeneratedConstructorSupport.accessibleConstructors(
                    definition.superclass(), definition.binaryName());
            JavaInteropAccess.ResolvedConstructor resolved =
                    interop.resolveConstructor(definition.superclass(), candidates, superArguments);
            int constructorIndex = candidates.indexOf(resolved.constructor());
            return new GeneratedConstructorSelection(constructorIndex, resolved.arguments());
        });
    }

    void finishConstructor(String descriptor, Object receiver, Object[] arguments) {
        session.execute(() -> {
            requireActive();
            GeneratedTypeDefinition.ConstructorDefinition constructor = constructors.get(descriptor);
            if (constructor == null) {
                throw new IllegalStateException("No generated constructor callback for " + descriptor);
            }
            BodySuperCall bodySuperCall = new BodySuperCall();
            Object[] callbackArguments = wrapArguments(arguments, 2);
            callbackArguments[0] = interop.wrapJavaValue(receiver, definition.binaryName());
            callbackArguments[1] = bodySuperCall;
            try {
                constructor.implementation().execute(callbackArguments);
                if (bodySuperCall.calls != 1) {
                    throw new IllegalStateException("Constructor must call $super(...) exactly once");
                }
            } catch (RuntimeException exception) {
                throw callbackFailure("constructor body " + descriptor, exception);
            }
            return null;
        });
    }

    Object fieldValue(int index) {
        return fieldValues.get(index);
    }

    void close() {
        active = false;
    }

    boolean belongsTo(JavaTypeGenerationSession candidateSession) {
        return session == candidateSession;
    }

    JavaTypeGenerationSession session() {
        return session;
    }

    private Object[] wrapArguments(Object[] arguments, int prefixLength) {
        Object[] wrapped = new Object[prefixLength + arguments.length];
        for (int index = 0; index < arguments.length; index++) {
            wrapped[prefixLength + index] = interop.wrapJavaValue(arguments[index]);
        }
        return wrapped;
    }

    private Object convertFieldValue(GeneratedTypeDefinition.FieldDefinition field) {
        if (!field.hasValue()) {
            return defaultValue(field.type());
        }
        return interop.convertValue(field.value(), field.type());
    }

    private void requireActive() {
        if (!active) {
            throw new IllegalStateException(
                    "Generated Java callbacks for package " + definition.packageId() + " are closed");
        }
    }

    private static String methodDescriptor(GeneratedTypeDefinition.MethodDefinition method) {
        Type[] argumentTypes =
                method.argumentTypes().stream().map(Type::getType).toArray(Type[]::new);
        return Type.getMethodDescriptor(Type.getType(method.returnType()), argumentTypes);
    }

    static String constructorDescriptor(List<Class<?>> argumentTypes) {
        Type[] types = argumentTypes.stream().map(Type::getType).toArray(Type[]::new);
        return Type.getMethodDescriptor(Type.VOID_TYPE, types);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Character.TYPE) {
            return '\0';
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Float.TYPE) {
            return 0F;
        }
        return 0D;
    }

    private static IllegalStateException callbackFailure(String description, RuntimeException exception) {
        return new IllegalStateException("Generated JavaScript " + description + " failed: " + exception, exception);
    }

    private static final class SuperRecorder implements ProxyExecutable {
        private Value[] arguments;

        @Override
        public Object execute(Value... arguments) {
            if (this.arguments != null) {
                throw new IllegalStateException("Constructor called $super(...) more than once");
            }
            this.arguments = arguments.clone();
            throw new SuperSelectionSignal();
        }
    }

    private static final class BodySuperCall implements ProxyExecutable {
        private int calls;

        @Override
        public Object execute(Value... arguments) {
            calls++;
            return null;
        }
    }

    private static final class SuperSelectionSignal extends RuntimeException {
        @Serial
        private static final long serialVersionUID = 1L;

        private SuperSelectionSignal() {
            super(null, null, false, false);
        }
    }

    private enum NoSelfProxy implements ProxyObject {
        INSTANCE;

        @Override
        public Object getMember(String key) {
            throw new IllegalStateException("$self cannot be used before $super(...)");
        }

        @Override
        public Object getMemberKeys() {
            return new String[0];
        }

        @Override
        public boolean hasMember(String key) {
            throw new IllegalStateException("$self cannot be used before $super(...)");
        }

        @Override
        public void putMember(String key, Value value) {
            throw new IllegalStateException("$self cannot be used before $super(...)");
        }
    }
}
