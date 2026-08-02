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

import io.github.trethore.myqolpackages.internal.mappings.MappingIndex;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import org.graalvm.polyglot.proxy.ProxyArray;

final class JavaInteropMembers {
  static final String CLASS_MEMBER = "_class";
  static final String EQUALS_MEMBER = "_equals";
  static final String INSTANCEOF_MEMBER = "_instanceof";
  static final String SELF_MEMBER = "_self";

  private static final String ARRAY_TYPE_SUFFIX = "[]";
  private static final String EXPLICIT_FIELD_ACCESS_SUFFIX = "$";
  private static final Set<String> INSTANCE_RESERVED_MEMBERS =
      Set.of(CLASS_MEMBER, EQUALS_MEMBER, INSTANCEOF_MEMBER, SELF_MEMBER);
  private static final String VARARGS_TYPE_SUFFIX = "...";

  private final List<Constructor<?>> constructors;
  private final Map<String, Field> instanceFields;
  private final ProxyArray instanceMemberKeys;
  private final Map<String, List<Method>> instanceMethods;
  private final Map<String, Field> staticFields;
  private final ProxyArray staticMemberKeys;
  private final Map<String, List<Method>> staticMethods;

  JavaInteropMembers(
      Class<?> targetClass, String preferredNamedClassName, MappingIndex mappingIndex) {
    Map<String, List<Method>> discoveredStaticMethods = new LinkedHashMap<>();
    Map<String, List<Method>> discoveredInstanceMethods = new LinkedHashMap<>();
    Map<String, Field> discoveredStaticFields = new LinkedHashMap<>();
    Map<String, Field> discoveredInstanceFields = new LinkedHashMap<>();
    List<Class<?>> hierarchy = collectHierarchy(targetClass);
    for (Class<?> currentClass : hierarchy) {
      MappingIndex.ClassMapping classMapping =
          resolveClassMapping(currentClass, targetClass, preferredNamedClassName, mappingIndex);
      collectMethods(
          currentClass,
          classMapping,
          mappingIndex,
          discoveredStaticMethods,
          discoveredInstanceMethods);
      collectFields(currentClass, classMapping, discoveredStaticFields, discoveredInstanceFields);
    }
    this.staticMethods = immutableMethodMap(discoveredStaticMethods);
    this.instanceMethods = immutableMethodMap(discoveredInstanceMethods);
    this.staticFields = Map.copyOf(discoveredStaticFields);
    this.instanceFields = Map.copyOf(discoveredInstanceFields);
    this.constructors =
        Arrays.stream(targetClass.getDeclaredConstructors())
            .sorted(JavaExecutableResolver.EXECUTABLE_COMPARATOR)
            .toList();
    this.staticMemberKeys = createMemberKeys(staticMethods, staticFields, false);
    this.instanceMemberKeys = createMemberKeys(instanceMethods, instanceFields, true);
  }

  static MemberName parseMemberName(String key) {
    if (key != null
        && key.endsWith(EXPLICIT_FIELD_ACCESS_SUFFIX)
        && key.length() > EXPLICIT_FIELD_ACCESS_SUFFIX.length()) {
      return new MemberName(
          key.substring(0, key.length() - EXPLICIT_FIELD_ACCESS_SUFFIX.length()), true);
    }
    return new MemberName(key, false);
  }

  static String explicitFieldName(String fieldName) {
    return fieldName + EXPLICIT_FIELD_ACCESS_SUFFIX;
  }

  static boolean isReservedInstanceMember(String key) {
    return INSTANCE_RESERVED_MEMBERS.contains(key);
  }

  List<Constructor<?>> constructors() {
    return constructors;
  }

  Map<String, Field> instanceFields() {
    return instanceFields;
  }

  ProxyArray instanceMemberKeys() {
    return instanceMemberKeys;
  }

  Map<String, List<Method>> instanceMethods() {
    return instanceMethods;
  }

  Map<String, Field> staticFields() {
    return staticFields;
  }

  ProxyArray staticMemberKeys() {
    return staticMemberKeys;
  }

  Map<String, List<Method>> staticMethods() {
    return staticMethods;
  }

  private static List<Class<?>> collectHierarchy(Class<?> targetClass) {
    List<Class<?>> hierarchy = new ArrayList<>();
    Queue<Class<?>> pending = new ArrayDeque<>();
    Set<Class<?>> visited = new HashSet<>();
    pending.add(targetClass);
    while (!pending.isEmpty()) {
      Class<?> currentClass = pending.remove();
      if (!visited.add(currentClass)) {
        continue;
      }
      hierarchy.add(currentClass);
      Class<?> superclass = currentClass.getSuperclass();
      if (superclass != null) {
        pending.add(superclass);
      }
      pending.addAll(List.of(currentClass.getInterfaces()));
    }
    return hierarchy;
  }

  private static MappingIndex.ClassMapping resolveClassMapping(
      Class<?> currentClass,
      Class<?> targetClass,
      String preferredNamedClassName,
      MappingIndex mappingIndex) {
    if (currentClass == targetClass && preferredNamedClassName != null) {
      MappingIndex.ClassMapping preferredMapping =
          mappingIndex.getClassMapping(preferredNamedClassName);
      if (preferredMapping != null) {
        return preferredMapping;
      }
    }
    return mappingIndex.findClassMapping(currentClass);
  }

  private static void collectMethods(
      Class<?> currentClass,
      MappingIndex.ClassMapping classMapping,
      MappingIndex mappingIndex,
      Map<String, List<Method>> staticMethods,
      Map<String, List<Method>> instanceMethods) {
    for (Method method : currentClass.getDeclaredMethods()) {
      Map<String, List<Method>> destination =
          Modifier.isStatic(method.getModifiers()) ? staticMethods : instanceMethods;
      addMethod(destination, method.getName(), method);
      if (classMapping == null) {
        continue;
      }
      for (Map.Entry<String, List<MappingIndex.MethodMapping>> mapping :
          classMapping.methodMappings().entrySet()) {
        if (mapping.getValue().stream()
            .anyMatch(methodMapping -> matches(method, methodMapping, mappingIndex))) {
          addMethod(destination, mapping.getKey(), method);
        }
      }
    }
  }

  private static void collectFields(
      Class<?> currentClass,
      MappingIndex.ClassMapping classMapping,
      Map<String, Field> staticFields,
      Map<String, Field> instanceFields) {
    for (Field field : currentClass.getDeclaredFields()) {
      Map<String, Field> destination =
          Modifier.isStatic(field.getModifiers()) ? staticFields : instanceFields;
      destination.putIfAbsent(field.getName(), field);
      if (classMapping == null) {
        continue;
      }
      for (Map.Entry<String, String> mapping : classMapping.fieldMappings().entrySet()) {
        if (mapping.getValue().equals(field.getName())) {
          destination.putIfAbsent(mapping.getKey(), field);
        }
      }
    }
  }

  private static void addMethod(
      Map<String, List<Method>> methods, String exposedName, Method method) {
    List<Method> candidates = methods.computeIfAbsent(exposedName, ignored -> new ArrayList<>());
    for (Method candidate : candidates) {
      if (Arrays.equals(candidate.getParameterTypes(), method.getParameterTypes())) {
        return;
      }
    }
    candidates.add(method);
  }

  private static boolean matches(
      Method method, MappingIndex.MethodMapping methodMapping, MappingIndex mappingIndex) {
    if (!method.getName().equals(methodMapping.runtimeName())) {
      return false;
    }
    Class<?>[] parameterTypes = method.getParameterTypes();
    if (parameterTypes.length != methodMapping.parameterTypes().size()) {
      return false;
    }
    for (int index = 0; index < parameterTypes.length; index++) {
      if (!matches(
          parameterTypes[index], methodMapping.parameterTypes().get(index), mappingIndex)) {
        return false;
      }
    }
    return true;
  }

  private static boolean matches(
      Class<?> runtimeType, String namedTypeName, MappingIndex mappingIndex) {
    String normalizedNamedTypeName = namedTypeName.replace(VARARGS_TYPE_SUFFIX, ARRAY_TYPE_SUFFIX);
    if (runtimeType.getTypeName().equals(normalizedNamedTypeName)) {
      return true;
    }
    int arrayDimensions = 0;
    String namedComponentType = normalizedNamedTypeName;
    while (namedComponentType.endsWith(ARRAY_TYPE_SUFFIX)) {
      arrayDimensions++;
      namedComponentType =
          namedComponentType.substring(0, namedComponentType.length() - ARRAY_TYPE_SUFFIX.length());
    }
    String runtimeComponentType = mappingIndex.getRuntimeClassName(namedComponentType);
    if (runtimeComponentType == null) {
      return false;
    }
    return runtimeType
        .getTypeName()
        .equals(runtimeComponentType + ARRAY_TYPE_SUFFIX.repeat(arrayDimensions));
  }

  private static Map<String, List<Method>> immutableMethodMap(Map<String, List<Method>> methods) {
    Map<String, List<Method>> immutableMethods = new LinkedHashMap<>();
    methods.forEach(
        (name, candidates) ->
            immutableMethods.put(
                name,
                candidates.stream().sorted(JavaExecutableResolver.EXECUTABLE_COMPARATOR).toList()));
    return Map.copyOf(immutableMethods);
  }

  private static ProxyArray createMemberKeys(
      Map<String, List<Method>> methods, Map<String, Field> fields, boolean instanceMembers) {
    Set<String> keys = new TreeSet<>(methods.keySet());
    for (String fieldName : fields.keySet()) {
      if (!methods.containsKey(fieldName)) {
        keys.add(fieldName);
      }
      keys.add(explicitFieldName(fieldName));
    }
    keys.add(CLASS_MEMBER);
    if (instanceMembers) {
      keys.addAll(INSTANCE_RESERVED_MEMBERS);
    }
    return ProxyArray.fromArray(keys.toArray());
  }

  record MemberName(String name, boolean explicitField) {}
}
