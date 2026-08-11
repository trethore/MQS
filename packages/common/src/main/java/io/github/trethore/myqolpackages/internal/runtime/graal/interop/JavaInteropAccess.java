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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop;

import io.github.trethore.myqolpackages.internal.runtime.graal.interop.mapping.MappingIndex;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public final class JavaInteropAccess {
    private final HostClassResolver resolver;

    JavaInteropAccess(HostClassResolver resolver) {
        this.resolver = resolver;
    }

    public Class<?> resolveClass(Value value) {
        return resolver.resolveClass(value);
    }

    public Object createClassProxy(String namedClassName, Class<?> targetClass) {
        return resolver.getOrCreateProxy(namedClassName, targetClass);
    }

    public Object wrapJavaValue(Object value) {
        return resolver.getInteropService().wrapJavaValue(value, null);
    }

    public Object wrapJavaValue(Object value, String preferredNamedClassName) {
        return resolver.getInteropService().wrapJavaValue(value, preferredNamedClassName);
    }

    public Object convertValue(Value value, Class<?> targetType) {
        return resolver.getInteropService().convertValue(value, targetType);
    }

    public ResolvedConstructor resolveConstructor(
            Class<?> targetClass, List<Constructor<?>> candidates, Value[] arguments) {
        JavaExecutableResolver.ResolvedExecutable<Constructor<?>> resolved =
                resolver.getInteropService().resolveConstructor(targetClass, candidates, arguments);
        return new ResolvedConstructor(resolved.executable(), resolved.arguments());
    }

    public ResolvedMethod resolveMethod(String methodName, List<Method> candidates, Value[] arguments) {
        JavaExecutableResolver.ResolvedExecutable<Method> resolved =
                resolver.getInteropService().resolveMethod(methodName, candidates, arguments);
        return new ResolvedMethod(resolved.executable(), resolved.arguments());
    }

    public List<Method> getInstanceMethods(Class<?> targetClass, String exposedName) {
        return resolver.getInteropService()
                .getMembers(targetClass, resolveNamedClassName(targetClass))
                .instanceMethods()
                .getOrDefault(exposedName, List.of());
    }

    public List<Method> getStaticMethods(Class<?> targetClass, String exposedName) {
        return resolver.getInteropService()
                .getMembers(targetClass, resolveNamedClassName(targetClass))
                .staticMethods()
                .getOrDefault(exposedName, List.of());
    }

    public Field getInstanceField(Class<?> targetClass, String exposedName) {
        return resolver.getInteropService()
                .getMembers(targetClass, resolveNamedClassName(targetClass))
                .instanceFields()
                .get(exposedName);
    }

    public MappingIndex mappings() {
        return resolver.getMappings();
    }

    private String resolveNamedClassName(Class<?> targetClass) {
        MappingIndex.ClassMapping mapping = resolver.getMappings().findClassMapping(targetClass);
        return mapping == null ? targetClass.getName() : mapping.namedClassName();
    }

    public record ResolvedConstructor(Constructor<?> constructor, Object[] arguments) {
        @Override
        public boolean equals(Object object) {
            return object instanceof ResolvedConstructor(Constructor<?> otherConstructor, Object[] otherArguments)
                    && constructor.equals(otherConstructor)
                    && Arrays.deepEquals(arguments, otherArguments);
        }

        @Override
        public int hashCode() {
            return 31 * constructor.hashCode() + Arrays.deepHashCode(arguments);
        }

        @Override
        public @NotNull String toString() {
            return "ResolvedConstructor[constructor="
                    + constructor
                    + ", arguments="
                    + Arrays.deepToString(arguments)
                    + "]";
        }
    }

    public record ResolvedMethod(Method method, Object[] arguments) {
        @Override
        public boolean equals(Object object) {
            return object instanceof ResolvedMethod(Method otherMethod, Object[] otherArguments)
                    && method.equals(otherMethod)
                    && Arrays.deepEquals(arguments, otherArguments);
        }

        @Override
        public int hashCode() {
            return 31 * method.hashCode() + Arrays.deepHashCode(arguments);
        }

        @Override
        public @NotNull String toString() {
            return "ResolvedMethod[method=" + method + ", arguments=" + Arrays.deepToString(arguments) + "]";
        }
    }
}
