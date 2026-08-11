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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

final class JavaObjectProxy implements ProxyObject {
    private final Object instance;
    private final JavaInteropMembers members;
    private final String preferredNamedClassName;
    private final JavaInteropService service;

    JavaObjectProxy(Object instance, String preferredNamedClassName, JavaInteropService service) {
        this.instance = instance;
        this.preferredNamedClassName = preferredNamedClassName;
        this.service = service;
        this.members = service.getMembers(instance.getClass(), preferredNamedClassName);
    }

    @Override
    public Object getMember(String key) {
        if (JavaInteropMembers.SELF_MEMBER.equals(key)) {
            return instance;
        }
        if (JavaInteropMembers.CLASS_MEMBER.equals(key)) {
            return instance.getClass();
        }
        if (JavaInteropMembers.EQUALS_MEMBER.equals(key)) {
            return (ProxyExecutable) arguments -> service.objectsEqual(instance, requireSingleArgument(key, arguments));
        }
        if (JavaInteropMembers.INSTANCEOF_MEMBER.equals(key)) {
            return (ProxyExecutable) arguments -> service.isInstance(instance, requireSingleArgument(key, arguments));
        }
        JavaInteropMembers.MemberName memberName = JavaInteropMembers.parseMemberName(key);
        if (memberName.explicitField()) {
            return readField(members.instanceFields().get(memberName.name()));
        }
        List<Method> methods = members.instanceMethods().get(memberName.name());
        if (methods != null) {
            return (ProxyExecutable) arguments ->
                    service.invokeMethod(instance, preferredNamedClassName, memberName.name(), methods, arguments);
        }
        return readField(members.instanceFields().get(memberName.name()));
    }

    @Override
    public Object getMemberKeys() {
        return members.instanceMemberKeys();
    }

    @Override
    public boolean hasMember(String key) {
        if (JavaInteropMembers.isReservedInstanceMember(key)) {
            return true;
        }
        JavaInteropMembers.MemberName memberName = JavaInteropMembers.parseMemberName(key);
        if (memberName.explicitField()) {
            return members.instanceFields().containsKey(memberName.name());
        }
        return members.instanceMethods().containsKey(memberName.name())
                || members.instanceFields().containsKey(memberName.name());
    }

    @Override
    public void putMember(String key, Value value) {
        if (JavaInteropMembers.isReservedInstanceMember(key)) {
            throw new UnsupportedOperationException("Cannot replace reserved member " + key);
        }
        JavaInteropMembers.MemberName memberName = JavaInteropMembers.parseMemberName(key);
        if (!memberName.explicitField() && members.instanceMethods().containsKey(memberName.name())) {
            throw new UnsupportedOperationException("Ambiguous write to member '"
                    + memberName.name()
                    + "'. Use "
                    + JavaInteropMembers.explicitFieldName(memberName.name())
                    + " to write the field");
        }
        Field field = members.instanceFields().get(memberName.name());
        service.writeField(instance, field, memberName.name(), value);
    }

    Object getInstance() {
        return instance;
    }

    private Object readField(Field field) {
        return field == null ? null : service.readField(instance, preferredNamedClassName, field);
    }

    private static Value requireSingleArgument(String member, Value[] arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException(member + " requires exactly one argument");
        }
        return arguments[0];
    }
}
