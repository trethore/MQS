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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.java.interop;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.graalvm.polyglot.Value;

final class JavaExecutableResolver {
    static final Comparator<Executable> EXECUTABLE_COMPARATOR =
            Comparator.comparingInt(Executable::getParameterCount).thenComparing(Executable::toGenericString);

    private final PolyglotValueConverter valueConverter = new PolyglotValueConverter();

    ResolvedExecutable<Constructor<?>> resolveConstructor(
            Class<?> targetClass, List<Constructor<?>> candidates, Value[] arguments) {
        ResolvedExecutable<Executable> resolved =
                resolve("constructor for " + targetClass.getName(), candidates, arguments);
        return new ResolvedExecutable<>((Constructor<?>) resolved.executable(), resolved.arguments());
    }

    ResolvedExecutable<Method> resolveMethod(String methodName, List<Method> candidates, Value[] arguments) {
        ResolvedExecutable<Executable> resolved = resolve("method " + methodName, candidates, arguments);
        return new ResolvedExecutable<>((Method) resolved.executable(), resolved.arguments());
    }

    Object convertValue(Value value, Class<?> targetType) {
        return valueConverter.convertValue(value, targetType);
    }

    private ResolvedExecutable<Executable> resolve(
            String description, List<? extends Executable> candidates, Value[] arguments) {
        List<ApplicableExecutable> applicableExecutables = new ArrayList<>();
        for (Executable candidate : candidates) {
            ApplicableExecutable applicable = evaluate(candidate, arguments);
            if (applicable != null) {
                applicableExecutables.add(applicable);
            }
        }
        if (applicableExecutables.isEmpty()) {
            throw new IllegalArgumentException(
                    "No applicable " + description + " for " + arguments.length + " argument(s)");
        }
        applicableExecutables.sort(Comparator.comparingInt(ApplicableExecutable::score)
                .thenComparing(ApplicableExecutable::varArgs)
                .thenComparing(ApplicableExecutable::executable, EXECUTABLE_COMPARATOR));
        ApplicableExecutable selected = selectMostSpecific(description, applicableExecutables);
        return new ResolvedExecutable<>(selected.executable(), selected.arguments());
    }

    private static ApplicableExecutable selectMostSpecific(
            String description, List<ApplicableExecutable> applicableExecutables) {
        ApplicableExecutable first = applicableExecutables.getFirst();
        int tiedCount = 1;
        while (tiedCount < applicableExecutables.size()) {
            ApplicableExecutable candidate = applicableExecutables.get(tiedCount);
            if (first.score() != candidate.score() || first.varArgs() != candidate.varArgs()) {
                break;
            }
            tiedCount++;
        }
        if (tiedCount == 1) {
            return first;
        }
        for (int candidateIndex = 0; candidateIndex < tiedCount; candidateIndex++) {
            ApplicableExecutable candidate = applicableExecutables.get(candidateIndex);
            boolean moreSpecificThanAll = true;
            for (int otherIndex = 0; otherIndex < tiedCount; otherIndex++) {
                if (candidateIndex != otherIndex
                        && !isMoreSpecific(
                                candidate.executable(),
                                applicableExecutables.get(otherIndex).executable())) {
                    moreSpecificThanAll = false;
                    break;
                }
            }
            if (moreSpecificThanAll) {
                return candidate;
            }
        }
        ApplicableExecutable second = applicableExecutables.get(1);
        throw new IllegalArgumentException("Ambiguous "
                + description
                + ": "
                + first.executable().toGenericString()
                + " and "
                + second.executable().toGenericString());
    }

    private ApplicableExecutable evaluate(Executable executable, Value[] arguments) {
        Class<?>[] parameterTypes = executable.getParameterTypes();
        int fixedParameterCount = executable.isVarArgs() ? parameterTypes.length - 1 : parameterTypes.length;
        if (arguments.length < fixedParameterCount
                || (!executable.isVarArgs() && arguments.length != parameterTypes.length)) {
            return null;
        }
        int score = executable.isVarArgs() ? 20 : 0;
        try {
            Object[] convertedArguments = new Object[parameterTypes.length];
            for (int index = 0; index < fixedParameterCount; index++) {
                PolyglotValueConverter.Conversion conversion =
                        valueConverter.convert(arguments[index], parameterTypes[index]);
                score += conversion.score();
                convertedArguments[index] = conversion.value();
            }
            if (executable.isVarArgs()) {
                Class<?> componentType = parameterTypes[fixedParameterCount].getComponentType();
                Object varargs = Array.newInstance(componentType, arguments.length - fixedParameterCount);
                for (int index = fixedParameterCount; index < arguments.length; index++) {
                    PolyglotValueConverter.Conversion conversion =
                            valueConverter.convert(arguments[index], componentType);
                    score += conversion.score();
                    Array.set(varargs, index - fixedParameterCount, conversion.value());
                }
                convertedArguments[fixedParameterCount] = varargs;
            }
            return new ApplicableExecutable(executable, convertedArguments, score);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static boolean isMoreSpecific(Executable first, Executable second) {
        Class<?>[] firstTypes = first.getParameterTypes();
        Class<?>[] secondTypes = second.getParameterTypes();
        if (firstTypes.length != secondTypes.length) {
            return false;
        }
        boolean moreSpecific = false;
        for (int index = 0; index < firstTypes.length; index++) {
            Class<?> firstType = PolyglotValueConverter.box(firstTypes[index]);
            Class<?> secondType = PolyglotValueConverter.box(secondTypes[index]);
            if (firstType.equals(secondType)) {
                continue;
            }
            if (!secondType.isAssignableFrom(firstType)) {
                return false;
            }
            moreSpecific = true;
        }
        return moreSpecific;
    }

    @SuppressWarnings({"java:S6206", "ClassCanBeRecord"})
    static final class ResolvedExecutable<T extends Executable> {
        private final Object[] arguments;
        private final T executable;

        private ResolvedExecutable(T executable, Object[] arguments) {
            this.executable = executable;
            this.arguments = arguments;
        }

        T executable() {
            return executable;
        }

        Object[] arguments() {
            return arguments;
        }
    }

    @SuppressWarnings({"java:S6206", "ClassCanBeRecord"})
    private static final class ApplicableExecutable {
        private final Object[] arguments;
        private final Executable executable;
        private final int score;

        private ApplicableExecutable(Executable executable, Object[] arguments, int score) {
            this.executable = executable;
            this.arguments = arguments;
            this.score = score;
        }

        private Executable executable() {
            return executable;
        }

        private Object[] arguments() {
            return arguments;
        }

        private int score() {
            return score;
        }

        private boolean varArgs() {
            return executable.isVarArgs();
        }
    }
}
