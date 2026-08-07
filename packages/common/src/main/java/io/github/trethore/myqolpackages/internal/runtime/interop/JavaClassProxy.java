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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;
import org.graalvm.polyglot.proxy.ProxyObject;

final class JavaClassProxy implements ProxyObject, ProxyInstantiable {
  private final JavaInteropMembers members;
  private final String namedClassName;
  private final JavaInteropService service;
  private final Class<?> targetClass;

  JavaClassProxy(String namedClassName, Class<?> targetClass, JavaInteropService service) {
    this.namedClassName = namedClassName;
    this.targetClass = targetClass;
    this.service = service;
    this.members = service.getMembers(targetClass, namedClassName);
  }

  @Override
  public Object newInstance(Value... arguments) {
    return service.instantiate(targetClass, namedClassName, members.constructors(), arguments);
  }

  @Override
  public Object getMember(String key) {
    if (JavaInteropMembers.CLASS_MEMBER.equals(key)) {
      return targetClass;
    }
    JavaInteropMembers.MemberName memberName = JavaInteropMembers.parseMemberName(key);
    if (memberName.explicitField()) {
      return readField(members.staticFields().get(memberName.name()));
    }
    List<Method> methods = members.staticMethods().get(memberName.name());
    if (methods != null) {
      return (ProxyExecutable)
          arguments ->
              service.invokeMethod(null, namedClassName, memberName.name(), methods, arguments);
    }
    return readField(members.staticFields().get(memberName.name()));
  }

  @Override
  public Object getMemberKeys() {
    return members.staticMemberKeys();
  }

  @Override
  public boolean hasMember(String key) {
    if (JavaInteropMembers.CLASS_MEMBER.equals(key)) {
      return true;
    }
    JavaInteropMembers.MemberName memberName = JavaInteropMembers.parseMemberName(key);
    if (memberName.explicitField()) {
      return members.staticFields().containsKey(memberName.name());
    }
    return members.staticMethods().containsKey(memberName.name())
        || members.staticFields().containsKey(memberName.name());
  }

  @Override
  public void putMember(String key, Value value) {
    if (JavaInteropMembers.CLASS_MEMBER.equals(key)) {
      throw new UnsupportedOperationException(
          "Cannot replace reserved member " + JavaInteropMembers.CLASS_MEMBER);
    }
    JavaInteropMembers.MemberName memberName = JavaInteropMembers.parseMemberName(key);
    if (!memberName.explicitField() && members.staticMethods().containsKey(memberName.name())) {
      throw new UnsupportedOperationException(
          "Ambiguous write to static member '"
              + memberName.name()
              + "'. Use "
              + JavaInteropMembers.explicitFieldName(memberName.name())
              + " to write the field");
    }
    Field field = members.staticFields().get(memberName.name());
    service.writeField(null, field, memberName.name(), value);
  }

  String getNamedClassName() {
    return namedClassName;
  }

  Class<?> getTargetClass() {
    return targetClass;
  }

  private Object readField(Field field) {
    return field == null ? null : service.readField(null, namedClassName, field);
  }

  @Override
  public String toString() {
    return namedClassName.equals(targetClass.getName())
        ? namedClassName
        : namedClassName + " -> " + targetClass.getName();
  }
}
