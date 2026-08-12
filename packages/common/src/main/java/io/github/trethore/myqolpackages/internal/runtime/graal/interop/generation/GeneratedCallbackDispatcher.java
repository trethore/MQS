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

import java.lang.reflect.Method;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

public final class GeneratedCallbackDispatcher {
    private GeneratedCallbackDispatcher() {}

    @SuppressWarnings("unused")
    @RuntimeType
    public static Object invoke(
            @Origin Method method, @This(optional = true) Object receiver, @AllArguments Object[] arguments) {
        return GeneratedTypeRegistry.INSTANCE
                .getBinding(method.getDeclaringClass())
                .invoke(method, receiver, arguments);
    }

    public static GeneratedConstructorSelection selectConstructor(
            Class<?> generatedClass, String descriptor, Object[] arguments) {
        return GeneratedTypeRegistry.INSTANCE.getBinding(generatedClass).selectConstructor(descriptor, arguments);
    }

    public static void finishConstructor(
            Class<?> generatedClass, String descriptor, Object receiver, Object[] arguments) {
        GeneratedTypeRegistry.INSTANCE.getBinding(generatedClass).finishConstructor(descriptor, receiver, arguments);
    }

    public static Object fieldValue(Class<?> generatedClass, int index) {
        return GeneratedTypeRegistry.INSTANCE.getBinding(generatedClass).fieldValue(index);
    }
}
