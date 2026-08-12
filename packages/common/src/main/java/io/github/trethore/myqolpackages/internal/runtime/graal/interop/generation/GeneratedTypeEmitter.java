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

import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.MethodManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.utility.nullability.NeverNull;

final class GeneratedTypeEmitter {
    private static final String CONSTRUCTOR_METHOD_NAME = "<init>";
    private static final Method FIELD_VALUE_METHOD;
    private static final Method FINISH_CONSTRUCTOR_METHOD;
    private static final Method SELECT_CONSTRUCTOR_METHOD;

    static {
        try {
            FIELD_VALUE_METHOD = GeneratedCallbackDispatcher.class.getMethod("fieldValue", Class.class, int.class);
            FINISH_CONSTRUCTOR_METHOD = GeneratedCallbackDispatcher.class.getMethod(
                    "finishConstructor", Class.class, String.class, Object.class, Object[].class);
            SELECT_CONSTRUCTOR_METHOD = GeneratedCallbackDispatcher.class.getMethod(
                    "selectConstructor", Class.class, String.class, Object[].class);
        } catch (NoSuchMethodException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    Class<?> emit(GeneratedTypeDefinition definition, ClassLoader classLoader) {
        DynamicType.Builder<?> builder = definition.kind() == GeneratedTypeDefinition.Kind.CLASS
                ? createClassBuilder(definition)
                : createInterfaceBuilder(definition);
        builder = defineFields(builder, definition);
        builder = defineMethods(builder, definition);
        if (definition.kind() == GeneratedTypeDefinition.Kind.CLASS) {
            builder = defineConstructors(builder, definition);
        }
        if (definition.fields().stream().anyMatch(GeneratedTypeDefinition.FieldDefinition::isStatic)) {
            builder = builder.invokable(isTypeInitializer())
                    .intercept(new Implementation.Simple(new StaticFieldInitializer(definition)));
        }
        try (DynamicType.Unloaded<?> unloadedType = builder.make();
                DynamicType.Loaded<?> loadedType =
                        unloadedType.load(classLoader, ClassLoadingStrategy.Default.INJECTION)) {
            return loadedType.getLoaded();
        }
    }

    private static DynamicType.Builder<?> createClassBuilder(GeneratedTypeDefinition definition) {
        DynamicType.Builder<?> builder = new ByteBuddy()
                .subclass(definition.superclass(), ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(definition.binaryName())
                .modifiers(Visibility.PUBLIC, definition.isFinal() ? TypeManifestation.FINAL : TypeManifestation.PLAIN);
        if (definition.isAbstract()) {
            builder = builder.modifiers(Visibility.PUBLIC, TypeManifestation.ABSTRACT);
        }
        if (!definition.interfaces().isEmpty()) {
            builder = builder.implement(definition.interfaces());
        }
        return builder;
    }

    private static DynamicType.Builder<?> createInterfaceBuilder(GeneratedTypeDefinition definition) {
        return new ByteBuddy()
                .makeInterface(definition.interfaces())
                .name(definition.binaryName())
                .modifiers(Visibility.PUBLIC, TypeManifestation.INTERFACE);
    }

    private static DynamicType.Builder<?> defineFields(
            DynamicType.Builder<?> builder, GeneratedTypeDefinition definition) {
        for (GeneratedTypeDefinition.FieldDefinition field : definition.fields()) {
            builder = builder.defineField(
                    field.name(),
                    field.type(),
                    field.visibility().byteBuddyVisibility(),
                    field.isStatic() ? Ownership.STATIC : Ownership.MEMBER,
                    field.isFinal() ? FieldManifestation.FINAL : FieldManifestation.PLAIN);
        }
        return builder;
    }

    private static DynamicType.Builder<?> defineMethods(
            DynamicType.Builder<?> builder, GeneratedTypeDefinition definition) {
        for (GeneratedTypeDefinition.MethodDefinition method : definition.methods()) {
            MethodManifestation manifestation;
            if (method.isAbstract()) {
                manifestation = MethodManifestation.ABSTRACT;
            } else if (method.isFinal()) {
                manifestation = MethodManifestation.FINAL;
            } else {
                manifestation = MethodManifestation.PLAIN;
            }
            DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial<?> initial = builder.defineMethod(
                    method.runtimeName(),
                    method.returnType(),
                    method.visibility().byteBuddyVisibility(),
                    method.isStatic() ? Ownership.STATIC : Ownership.MEMBER,
                    manifestation);
            DynamicType.Builder.MethodDefinition.ImplementationDefinition<?> implementation =
                    initial.withParameters(method.argumentTypes());
            builder = method.isAbstract()
                    ? implementation.withoutCode()
                    : implementation.intercept(MethodDelegation.to(GeneratedCallbackDispatcher.class));
        }
        return builder;
    }

    private static DynamicType.Builder<?> defineConstructors(
            DynamicType.Builder<?> builder, GeneratedTypeDefinition definition) {
        List<GeneratedTypeDefinition.FieldDefinition> instanceFields =
                definition.fields().stream().filter(field -> !field.isStatic()).toList();
        if (definition.constructors().isEmpty()) {
            Constructor<?> superConstructor =
                    GeneratedConstructorSupport.accessibleConstructors(definition.superclass(), definition.binaryName())
                            .stream()
                            .filter(constructor -> constructor.getParameterCount() == 0)
                            .findFirst()
                            .orElseThrow();
            return builder.defineConstructor(Visibility.PUBLIC)
                    .intercept(new Implementation.Simple(
                            new FixedConstructorAppender(definition, superConstructor, instanceFields)));
        }
        for (GeneratedTypeDefinition.ConstructorDefinition constructor : definition.constructors()) {
            builder = builder.defineConstructor(constructor.visibility().byteBuddyVisibility())
                    .withParameters(constructor.argumentTypes())
                    .intercept(new Implementation.Simple(
                            new DynamicConstructorAppender(definition, constructor, instanceFields)));
        }
        return builder;
    }

    // Bytecode appenders use identity semantics as behavioral strategy objects.
    @SuppressWarnings("ClassCanBeRecord")
    private static final class StaticFieldInitializer implements ByteCodeAppender {
        private final GeneratedTypeDefinition definition;

        private StaticFieldInitializer(GeneratedTypeDefinition definition) {
            this.definition = definition;
        }

        @Override
        @NeverNull
        public Size apply(
                @NeverNull MethodVisitor methodVisitor,
                @NeverNull Implementation.Context implementationContext,
                @NeverNull MethodDescription instrumentedMethod) {
            for (int index = 0; index < definition.fields().size(); index++) {
                GeneratedTypeDefinition.FieldDefinition field =
                        definition.fields().get(index);
                if (!field.isStatic()) {
                    continue;
                }
                methodVisitor.visitLdcInsn(Type.getObjectType(internalName(definition.binaryName())));
                methodVisitor.visitLdcInsn(index);
                methodVisitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(GeneratedCallbackDispatcher.class),
                        FIELD_VALUE_METHOD.getName(),
                        Type.getMethodDescriptor(FIELD_VALUE_METHOD),
                        false);
                castOrUnbox(methodVisitor, field.type());
                methodVisitor.visitFieldInsn(
                        Opcodes.PUTSTATIC,
                        internalName(definition.binaryName()),
                        field.name(),
                        Type.getDescriptor(field.type()));
            }
            methodVisitor.visitInsn(Opcodes.RETURN);
            return new Size(4, 0);
        }
    }

    private abstract static class ConstructorAppender implements ByteCodeAppender {
        final GeneratedTypeDefinition definition;
        final List<GeneratedTypeDefinition.FieldDefinition> instanceFields;

        ConstructorAppender(
                GeneratedTypeDefinition definition, List<GeneratedTypeDefinition.FieldDefinition> instanceFields) {
            this.definition = definition;
            this.instanceFields = instanceFields;
        }

        final void initializeFields(MethodVisitor methodVisitor) {
            for (GeneratedTypeDefinition.FieldDefinition field : instanceFields) {
                int index = definition.fields().indexOf(field);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitLdcInsn(Type.getObjectType(internalName(definition.binaryName())));
                methodVisitor.visitLdcInsn(index);
                methodVisitor.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(GeneratedCallbackDispatcher.class),
                        FIELD_VALUE_METHOD.getName(),
                        Type.getMethodDescriptor(FIELD_VALUE_METHOD),
                        false);
                castOrUnbox(methodVisitor, field.type());
                methodVisitor.visitFieldInsn(
                        Opcodes.PUTFIELD,
                        internalName(definition.binaryName()),
                        field.name(),
                        Type.getDescriptor(field.type()));
            }
        }

        final void createArgumentArray(MethodVisitor methodVisitor, List<Class<?>> argumentTypes, int arrayLocal) {
            methodVisitor.visitLdcInsn(argumentTypes.size());
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class));
            int local = 1;
            for (int index = 0; index < argumentTypes.size(); index++) {
                Class<?> argumentType = argumentTypes.get(index);
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(index);
                Type asmType = Type.getType(argumentType);
                methodVisitor.visitVarInsn(asmType.getOpcode(Opcodes.ILOAD), local);
                box(methodVisitor, argumentType);
                methodVisitor.visitInsn(Opcodes.AASTORE);
                local += asmType.getSize();
            }
            methodVisitor.visitVarInsn(Opcodes.ASTORE, arrayLocal);
        }

        private static void box(MethodVisitor methodVisitor, Class<?> type) {
            if (!type.isPrimitive()) {
                return;
            }
            Class<?> boxed = boxedType(type);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(boxed),
                    "valueOf",
                    "(" + Type.getDescriptor(type) + ")" + Type.getDescriptor(boxed),
                    false);
        }
    }

    private static final class FixedConstructorAppender extends ConstructorAppender {
        private final Constructor<?> superConstructor;

        private FixedConstructorAppender(
                GeneratedTypeDefinition definition,
                Constructor<?> superConstructor,
                List<GeneratedTypeDefinition.FieldDefinition> instanceFields) {
            super(definition, instanceFields);
            this.superConstructor = superConstructor;
        }

        @Override
        @NeverNull
        public Size apply(
                @NeverNull MethodVisitor methodVisitor,
                @NeverNull Implementation.Context implementationContext,
                @NeverNull MethodDescription instrumentedMethod) {
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    Type.getInternalName(definition.superclass()),
                    CONSTRUCTOR_METHOD_NAME,
                    Type.getConstructorDescriptor(superConstructor),
                    false);
            initializeFields(methodVisitor);
            methodVisitor.visitInsn(Opcodes.RETURN);
            return new Size(8, 1);
        }
    }

    private static final class DynamicConstructorAppender extends ConstructorAppender {
        private final GeneratedTypeDefinition.ConstructorDefinition constructor;

        private DynamicConstructorAppender(
                GeneratedTypeDefinition definition,
                GeneratedTypeDefinition.ConstructorDefinition constructor,
                List<GeneratedTypeDefinition.FieldDefinition> instanceFields) {
            super(definition, instanceFields);
            this.constructor = constructor;
        }

        @Override
        @NeverNull
        public Size apply(
                @NeverNull MethodVisitor methodVisitor,
                @NeverNull Implementation.Context implementationContext,
                @NeverNull MethodDescription instrumentedMethod) {
            int arrayLocal = firstFreeLocal(constructor.argumentTypes());
            int selectionLocal = arrayLocal + 1;
            createArgumentArray(methodVisitor, constructor.argumentTypes(), arrayLocal);
            String descriptor = GeneratedCallbackBinding.constructorDescriptor(constructor.argumentTypes());
            methodVisitor.visitLdcInsn(Type.getObjectType(internalName(definition.binaryName())));
            methodVisitor.visitLdcInsn(descriptor);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, arrayLocal);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(GeneratedCallbackDispatcher.class),
                    SELECT_CONSTRUCTOR_METHOD.getName(),
                    Type.getMethodDescriptor(SELECT_CONSTRUCTOR_METHOD),
                    false);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, selectionLocal);

            List<Constructor<?>> superConstructors = GeneratedConstructorSupport.accessibleConstructors(
                    definition.superclass(), definition.binaryName());
            Label initialized = new Label();
            Label invalid = new Label();
            Label[] cases = new Label[superConstructors.size()];
            for (int index = 0; index < cases.length; index++) {
                cases[index] = new Label();
            }
            methodVisitor.visitVarInsn(Opcodes.ALOAD, selectionLocal);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(GeneratedConstructorSelection.class),
                    "constructorIndex",
                    "()I",
                    false);
            methodVisitor.visitTableSwitchInsn(0, cases.length - 1, invalid, cases);
            Object[] uninitializedLocals =
                    constructorFrameLocals(constructor.argumentTypes(), false, definition.binaryName());
            for (int constructorIndex = 0; constructorIndex < superConstructors.size(); constructorIndex++) {
                Constructor<?> superConstructor = superConstructors.get(constructorIndex);
                methodVisitor.visitLabel(cases[constructorIndex]);
                methodVisitor.visitFrame(
                        Opcodes.F_FULL, uninitializedLocals.length, uninitializedLocals, 0, new Object[0]);
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                Class<?>[] parameterTypes = superConstructor.getParameterTypes();
                for (int argumentIndex = 0; argumentIndex < parameterTypes.length; argumentIndex++) {
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, selectionLocal);
                    methodVisitor.visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            Type.getInternalName(GeneratedConstructorSelection.class),
                            "arguments",
                            "()[Ljava/lang/Object;",
                            false);
                    methodVisitor.visitLdcInsn(argumentIndex);
                    methodVisitor.visitInsn(Opcodes.AALOAD);
                    castOrUnbox(methodVisitor, parameterTypes[argumentIndex]);
                }
                methodVisitor.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        Type.getInternalName(definition.superclass()),
                        CONSTRUCTOR_METHOD_NAME,
                        Type.getConstructorDescriptor(superConstructor),
                        false);
                methodVisitor.visitJumpInsn(Opcodes.GOTO, initialized);
            }
            methodVisitor.visitLabel(invalid);
            methodVisitor.visitFrame(Opcodes.F_FULL, uninitializedLocals.length, uninitializedLocals, 0, new Object[0]);
            methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalStateException.class));
            methodVisitor.visitInsn(Opcodes.DUP);
            methodVisitor.visitLdcInsn("Invalid selected superclass constructor");
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    Type.getInternalName(IllegalStateException.class),
                    CONSTRUCTOR_METHOD_NAME,
                    "(Ljava/lang/String;)V",
                    false);
            methodVisitor.visitInsn(Opcodes.ATHROW);

            methodVisitor.visitLabel(initialized);
            Object[] initializedLocals =
                    constructorFrameLocals(constructor.argumentTypes(), true, definition.binaryName());
            methodVisitor.visitFrame(Opcodes.F_FULL, initializedLocals.length, initializedLocals, 0, new Object[0]);
            initializeFields(methodVisitor);
            methodVisitor.visitLdcInsn(Type.getObjectType(internalName(definition.binaryName())));
            methodVisitor.visitLdcInsn(descriptor);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, arrayLocal);
            methodVisitor.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(GeneratedCallbackDispatcher.class),
                    FINISH_CONSTRUCTOR_METHOD.getName(),
                    Type.getMethodDescriptor(FINISH_CONSTRUCTOR_METHOD),
                    false);
            methodVisitor.visitInsn(Opcodes.RETURN);
            return new Size(16 + constructor.argumentTypes().size() * 2, selectionLocal + 1);
        }

        private static int firstFreeLocal(List<Class<?>> argumentTypes) {
            int local = 1;
            for (Class<?> argumentType : argumentTypes) {
                local += Type.getType(argumentType).getSize();
            }
            return local;
        }

        private static Object[] constructorFrameLocals(
                List<Class<?>> argumentTypes, boolean initialized, String binaryName) {
            List<Object> locals = new ArrayList<>();
            locals.add(initialized ? internalName(binaryName) : Opcodes.UNINITIALIZED_THIS);
            for (Class<?> argumentType : argumentTypes) {
                locals.add(frameType(argumentType));
            }
            locals.add("[Ljava/lang/Object;");
            locals.add(Type.getInternalName(GeneratedConstructorSelection.class));
            return locals.toArray();
        }

        private static Object frameType(Class<?> type) {
            if (!type.isPrimitive()) {
                return Type.getType(type).getInternalName();
            }
            if (type == Float.TYPE) {
                return Opcodes.FLOAT;
            }
            if (type == Long.TYPE) {
                return Opcodes.LONG;
            }
            if (type == Double.TYPE) {
                return Opcodes.DOUBLE;
            }
            return Opcodes.INTEGER;
        }
    }

    private static void castOrUnbox(MethodVisitor methodVisitor, Class<?> type) {
        if (!type.isPrimitive()) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getType(type).getInternalName());
            return;
        }
        Class<?> boxed = boxedType(type);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(boxed));
        String methodName;
        if (type == Boolean.TYPE) {
            methodName = "booleanValue";
        } else if (type == Character.TYPE) {
            methodName = "charValue";
        } else {
            methodName = type.getName() + "Value";
        }
        methodVisitor.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL, Type.getInternalName(boxed), methodName, "()" + Type.getDescriptor(type), false);
    }

    private static Class<?> boxedType(Class<?> type) {
        if (type == Boolean.TYPE) {
            return Boolean.class;
        }
        if (type == Byte.TYPE) {
            return Byte.class;
        }
        if (type == Short.TYPE) {
            return Short.class;
        }
        if (type == Integer.TYPE) {
            return Integer.class;
        }
        if (type == Long.TYPE) {
            return Long.class;
        }
        if (type == Float.TYPE) {
            return Float.class;
        }
        if (type == Double.TYPE) {
            return Double.class;
        }
        if (type == Character.TYPE) {
            return Character.class;
        }
        throw new IllegalArgumentException("Unsupported primitive type " + type.getTypeName());
    }

    private static String internalName(String binaryName) {
        return binaryName.replace('.', '/');
    }
}
