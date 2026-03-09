/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Rethore
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

package net.me.scripting.typings;

import net.me.scripting.typings.schema.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

import static net.me.scripting.typings.schema.TsDescriptors.fn;
import static net.me.scripting.typings.schema.TsDescriptors.ro;

public final class ReflectedJavaTypeDescriptors {
    private ReflectedJavaTypeDescriptors() {
    }

    public static TsObject describeStaticClass(String objectName, Class<?> javaClass) {
        List<TsMember> members = new ArrayList<>();

        for (Field field : collectStaticFields(javaClass)) {
            members.add(ro(field.getName(), renderJavaType(field.getType())));
        }

        for (Map.Entry<String, List<Method>> methodEntry : collectStaticMethods(javaClass).entrySet()) {
            List<TsFunction> overloads = new ArrayList<>();
            for (Method method : methodEntry.getValue()) {
                overloads.add(renderMethod(method));
            }
            members.add(new TsMethod(methodEntry.getKey(), List.copyOf(overloads)));
        }

        return new TsObject(objectName, List.copyOf(members));
    }

    private static List<Field> collectStaticFields(Class<?> javaClass) {
        List<Field> fields = new ArrayList<>();
        for (Field field : javaClass.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            fields.add(field);
        }
        fields.sort(Comparator.comparing(Field::getName));
        return fields;
    }

    private static Map<String, List<Method>> collectStaticMethods(Class<?> javaClass) {
        Map<String, List<Method>> methodsByName = new LinkedHashMap<>();
        List<Method> methods = new ArrayList<>();
        for (Method method : javaClass.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass() != Object.class) {
                methods.add(method);
            }
        }

        methods.sort(
                Comparator.comparing(Method::getName)
                        .thenComparingInt(Method::getParameterCount)
                        .thenComparing(ReflectedJavaTypeDescriptors::renderMethodSignature)
        );

        for (Method method : methods) {
            methodsByName.computeIfAbsent(method.getName(), ignored -> new ArrayList<>()).add(method);
        }
        return methodsByName;
    }

    private static TsFunction renderMethod(Method method) {
        Parameter[] parameters = method.getParameters();
        List<TsParam> params = new ArrayList<>(parameters.length);
        for (int index = 0; index < parameters.length; index++) {
            params.add(new TsParam(resolveParameterName(parameters[index], index), renderJavaType(parameters[index].getType()), false, false));
        }
        return fn(renderJavaType(method.getReturnType()), params.toArray(TsParam[]::new));
    }

    private static String resolveParameterName(Parameter parameter, int index) {
        String reflectedName = parameter.getName();
        if (TypingsDeclarationUtils.isAccessibleIdentifier(reflectedName)) {
            return reflectedName;
        }
        return "arg" + index;
    }

    private static String renderMethodSignature(Method method) {
        StringBuilder builder = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int index = 0; index < parameterTypes.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(parameterTypes[index].getName());
        }
        return builder.append(')').toString();
    }

    private static String renderJavaType(Class<?> type) {
        if (type == null) {
            return "any";
        }
        if (type == Void.TYPE) {
            return TypingsConstants.VOID;
        }
        if (type == Boolean.TYPE || type == Boolean.class) {
            return TypingsConstants.BOOLEAN;
        }
        if (type == Character.TYPE || type == Character.class || type == String.class || CharSequence.class.isAssignableFrom(type)) {
            return TypingsConstants.STRING;
        }
        if (type.isPrimitive() || Number.class.isAssignableFrom(type)) {
            return TypingsConstants.NUMBER;
        }
        if (type.isArray()) {
            return "Array<" + renderJavaType(type.getComponentType()) + ">";
        }
        if (type == Object.class) {
            return "any";
        }
        if (type == Class.class) {
            return TypingsConstants.JAVA_CLASS_ANY;
        }
        if (Enum.class.isAssignableFrom(type)) {
            return TypingsConstants.STRING + " | " + TypingsConstants.NUMBER;
        }
        return "JavaInstance | any";
    }
}
