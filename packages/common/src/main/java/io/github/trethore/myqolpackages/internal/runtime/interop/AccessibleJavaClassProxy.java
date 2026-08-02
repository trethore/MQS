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

final class AccessibleJavaClassProxy extends JavaClassProxy implements ProxyInstantiable {
  private static final String CLASS_MEMBER = "_class";

  private final JavaInteropMembers members;
  private final JavaInteropService service;

  AccessibleJavaClassProxy(
      String namedClassName, Class<?> targetClass, JavaInteropService service) {
    super(namedClassName, targetClass);
    this.service = service;
    this.members = service.getMembers(targetClass, namedClassName);
  }

  @Override
  public Object newInstance(Value... arguments) {
    return service.instantiate(
        getTargetClass(), getNamedClassName(), members.constructors(), arguments);
  }

  @Override
  public Object getMember(String key) {
    if (CLASS_MEMBER.equals(key)) {
      return getTargetClass();
    }
    JavaInteropMembers.MemberName memberName = JavaInteropMembers.parseMemberName(key);
    if (memberName.explicitField()) {
      return readField(members.staticFields().get(memberName.name()));
    }
    List<Method> methods = members.staticMethods().get(memberName.name());
    if (methods != null) {
      return (ProxyExecutable)
          arguments ->
              service.invokeMethod(
                  null, getNamedClassName(), memberName.name(), methods, arguments);
    }
    return readField(members.staticFields().get(memberName.name()));
  }

  @Override
  public Object getMemberKeys() {
    return members.staticMemberKeys();
  }

  @Override
  public boolean hasMember(String key) {
    if (CLASS_MEMBER.equals(key)) {
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
    if (CLASS_MEMBER.equals(key)) {
      throw new UnsupportedOperationException("Cannot replace reserved member _class");
    }
    JavaInteropMembers.MemberName memberName = JavaInteropMembers.parseMemberName(key);
    if (!memberName.explicitField() && members.staticMethods().containsKey(memberName.name())) {
      throw new UnsupportedOperationException(
          "Ambiguous write to static member '"
              + memberName.name()
              + "'. Use "
              + memberName.name()
              + "$ to write the field");
    }
    Field field = members.staticFields().get(memberName.name());
    service.writeField(null, field, memberName.name(), value);
  }

  private Object readField(Field field) {
    if (field == null) {
      return null;
    }
    return service.readField(null, getNamedClassName(), field);
  }
}
