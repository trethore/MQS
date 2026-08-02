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
package io.github.trethore.myqolpackages.internal.runtime.interop;

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
      Comparator.comparingInt(Executable::getParameterCount)
          .thenComparing(Executable::toGenericString);

  ResolvedExecutable<Constructor<?>> resolveConstructor(
      Class<?> targetClass, List<Constructor<?>> candidates, Value[] arguments) {
    ResolvedExecutable<Executable> resolved =
        resolve("constructor for " + targetClass.getName(), candidates, arguments);
    return new ResolvedExecutable<>((Constructor<?>) resolved.executable(), resolved.arguments());
  }

  ResolvedExecutable<Method> resolveMethod(
      String methodName, List<Method> candidates, Value[] arguments) {
    ResolvedExecutable<Executable> resolved =
        resolve("method " + methodName, candidates, arguments);
    return new ResolvedExecutable<>((Method) resolved.executable(), resolved.arguments());
  }

  Object convertValue(Value value, Class<?> targetType) {
    return convert(value, targetType).value();
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
    applicableExecutables.sort(
        Comparator.comparingInt(ApplicableExecutable::score)
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
                candidate.executable(), applicableExecutables.get(otherIndex).executable())) {
          moreSpecificThanAll = false;
          break;
        }
      }
      if (moreSpecificThanAll) {
        return candidate;
      }
    }
    ApplicableExecutable second = applicableExecutables.get(1);
    throw new IllegalArgumentException(
        "Ambiguous "
            + description
            + ": "
            + first.executable().toGenericString()
            + " and "
            + second.executable().toGenericString());
  }

  private ApplicableExecutable evaluate(Executable executable, Value[] arguments) {
    Class<?>[] parameterTypes = executable.getParameterTypes();
    int fixedParameterCount =
        executable.isVarArgs() ? parameterTypes.length - 1 : parameterTypes.length;
    if (arguments.length < fixedParameterCount
        || (!executable.isVarArgs() && arguments.length != parameterTypes.length)) {
      return null;
    }
    int score = executable.isVarArgs() ? 20 : 0;
    try {
      Object[] convertedArguments = new Object[parameterTypes.length];
      for (int index = 0; index < fixedParameterCount; index++) {
        Conversion conversion = convert(arguments[index], parameterTypes[index]);
        score += conversion.score();
        convertedArguments[index] = conversion.value();
      }
      if (executable.isVarArgs()) {
        Class<?> componentType = parameterTypes[fixedParameterCount].getComponentType();
        Object varargs = Array.newInstance(componentType, arguments.length - fixedParameterCount);
        for (int index = fixedParameterCount; index < arguments.length; index++) {
          Conversion conversion = convert(arguments[index], componentType);
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

  private Conversion convert(Value value, Class<?> targetType) {
    if (value == null || value.isNull()) {
      return convertNull(targetType);
    }
    Conversion referenceConversion = convertReference(value, targetType);
    if (referenceConversion != null) {
      return referenceConversion;
    }
    Class<?> boxedType = box(targetType);
    Conversion builtInConversion = convertBuiltIn(value, boxedType);
    if (builtInConversion != null) {
      return builtInConversion;
    }
    if (targetType == Object.class) {
      return new Conversion(convertGenericValue(value), 40);
    }
    return convertWithPolyglot(value, targetType);
  }

  private static Conversion convertNull(Class<?> targetType) {
    if (targetType.isPrimitive()) {
      throw new IllegalArgumentException("null cannot be converted to " + targetType.getName());
    }
    return new Conversion(null, 5);
  }

  private static Conversion convertReference(Value value, Class<?> targetType) {
    if (targetType == Value.class) {
      return new Conversion(value, 30);
    }
    Object proxyValue = unwrapProxy(value);
    if (targetType.isInstance(proxyValue)) {
      return new Conversion(proxyValue, proxyValue.getClass() == targetType ? 0 : 2);
    }
    if (value.isHostObject()) {
      Object hostObject = value.asHostObject();
      if (targetType.isInstance(hostObject)) {
        return new Conversion(hostObject, hostObject.getClass() == targetType ? 0 : 2);
      }
    }
    return null;
  }

  private static Conversion convertBuiltIn(Value value, Class<?> boxedType) {
    if (boxedType == Boolean.class && value.isBoolean()) {
      return new Conversion(value.asBoolean(), 0);
    }
    if (boxedType == Character.class && value.isString()) {
      String stringValue = value.asString();
      if (stringValue.length() == 1) {
        return new Conversion(stringValue.charAt(0), 1);
      }
      throw new IllegalArgumentException("String is not one character");
    }
    if ((boxedType == String.class || boxedType == CharSequence.class) && value.isString()) {
      return new Conversion(value.asString(), boxedType == String.class ? 0 : 1);
    }
    if (boxedType.isEnum() && value.isString()) {
      return new Conversion(convertEnum(boxedType, value.asString()), 3);
    }
    if (Number.class.isAssignableFrom(boxedType) && value.isNumber()) {
      return convertNumber(value, boxedType);
    }
    return null;
  }

  private static Conversion convertWithPolyglot(Value value, Class<?> targetType) {
    try {
      return new Conversion(value.as(targetType), 25);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException(
          "Value cannot be converted to " + targetType.getTypeName(), exception);
    }
  }

  private static Object unwrapProxy(Value value) {
    if (!value.isProxyObject()) {
      return null;
    }
    Object proxy = value.asProxyObject();
    if (proxy instanceof JavaObjectProxy objectProxy) {
      return objectProxy.getInstance();
    }
    if (proxy instanceof JavaClassProxy classProxy) {
      return classProxy.getTargetClass();
    }
    return null;
  }

  private static Conversion convertNumber(Value value, Class<?> boxedType) {
    if (boxedType == Byte.class && value.fitsInByte()) {
      return new Conversion(value.asByte(), 1);
    }
    if (boxedType == Short.class && value.fitsInShort()) {
      return new Conversion(value.asShort(), 2);
    }
    if (boxedType == Integer.class && value.fitsInInt()) {
      return new Conversion(value.asInt(), 3);
    }
    if (boxedType == Long.class && value.fitsInLong()) {
      return new Conversion(value.asLong(), 4);
    }
    if (boxedType == Float.class && value.fitsInFloat()) {
      return new Conversion(value.asFloat(), 5);
    }
    if (boxedType == Double.class && value.fitsInDouble()) {
      return new Conversion(value.asDouble(), 6);
    }
    if (boxedType == Number.class && value.fitsInDouble()) {
      return new Conversion(value.asDouble(), 10);
    }
    throw new IllegalArgumentException("Number does not fit in " + boxedType.getName());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object convertEnum(Class<?> enumType, String name) {
    return Enum.valueOf((Class<? extends Enum>) enumType, name);
  }

  private static Object convertGenericValue(Value value) {
    if (value.isBoolean()) {
      return value.asBoolean();
    }
    if (value.isString()) {
      return value.asString();
    }
    if (value.isNumber()) {
      return value.asDouble();
    }
    if (value.isHostObject()) {
      return value.asHostObject();
    }
    if (value.isProxyObject()) {
      return value.asProxyObject();
    }
    return value;
  }

  private static boolean isMoreSpecific(Executable first, Executable second) {
    Class<?>[] firstTypes = first.getParameterTypes();
    Class<?>[] secondTypes = second.getParameterTypes();
    if (firstTypes.length != secondTypes.length) {
      return false;
    }
    boolean moreSpecific = false;
    for (int index = 0; index < firstTypes.length; index++) {
      Class<?> firstType = box(firstTypes[index]);
      Class<?> secondType = box(secondTypes[index]);
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

  private static Class<?> box(Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    if (type == boolean.class) {
      return Boolean.class;
    }
    if (type == byte.class) {
      return Byte.class;
    }
    if (type == short.class) {
      return Short.class;
    }
    if (type == int.class) {
      return Integer.class;
    }
    if (type == long.class) {
      return Long.class;
    }
    if (type == float.class) {
      return Float.class;
    }
    if (type == double.class) {
      return Double.class;
    }
    if (type == char.class) {
      return Character.class;
    }
    return Void.class;
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

  private record Conversion(Object value, int score) {}
}
