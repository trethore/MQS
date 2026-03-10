/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
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

package net.me.scripting.typings;

import net.me.event.FabricEventRegistry;
import net.me.scripting.typings.schema.TsMember;
import net.me.scripting.typings.schema.TsObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static net.me.scripting.typings.schema.TsDescriptors.alias;
import static net.me.scripting.typings.schema.TsDescriptors.ro;

public final class FabricEventTypeDescriptors {
    public static final String ROOT_OBJECT_NAME = "MQSFabricEventsApi";
    public static final String EVENT_TYPE_NAME = "MQSFabricEvent";
    public static final String EVENT_CALLBACK_TYPE_NAME = "MQSFabricEventCallback";
    public static final String EVENT_TYPE_BOUND = EVENT_TYPE_NAME + "<Array<unknown>, unknown>";

    private FabricEventTypeDescriptors() {
    }

    public static MqsApiFragment describeTypeScript() {
        FabricTypeNode root = new FabricTypeNode(ROOT_OBJECT_NAME);

        for (FabricEventRegistry.FabricEventEntry entry : FabricEventRegistry.discoverEntries()) {
            registerEntry(root, entry);
        }

        List<TsObject> objects = new ArrayList<>();
        appendObjects(root, objects);

        return new MqsApiFragment(
                List.of(
                        alias(EVENT_TYPE_NAME, "<TArgs extends Array<unknown> = Array<unknown>, TReturn = unknown>",
                                "JavaInstance & { readonly __mqsFabricArgs?: TArgs; readonly __mqsFabricReturn?: TReturn; }"),
                        alias(EVENT_CALLBACK_TYPE_NAME, "<TEvent extends " + EVENT_TYPE_BOUND + ">",
                                "TEvent extends " + EVENT_TYPE_NAME + "<infer TArgs, infer TReturn> ? (...args: TArgs) => TReturn : " + TypingsConstants.EVENT_CALLBACK)
                ),
                List.of(),
                List.of(),
                List.copyOf(objects)
        );
    }

    private static void registerEntry(FabricTypeNode root, FabricEventRegistry.FabricEventEntry entry) {
        root.register(entry.pathSegments(), entry.fieldName(), renderEventType(entry.listenerMethod()));
    }

    private static void appendObjects(FabricTypeNode node, List<TsObject> objects) {
        List<TsMember> members = new ArrayList<>(node.childrenByName.size() + node.eventsByName.size());

        for (Map.Entry<String, FabricTypeNode> childEntry : node.childrenByName.entrySet()) {
            members.add(ro(childEntry.getKey(), childEntry.getValue().interfaceName));
        }
        for (Map.Entry<String, String> eventEntry : node.eventsByName.entrySet()) {
            members.add(ro(eventEntry.getKey(), eventEntry.getValue()));
        }

        objects.add(new TsObject(node.interfaceName, List.copyOf(members)));
        for (FabricTypeNode child : node.childrenByName.values()) {
            appendObjects(child, objects);
        }
    }

    private static String renderEventType(Method listenerMethod) {
        return EVENT_TYPE_NAME + "<" + renderTupleType(listenerMethod.getParameterTypes()) + ", "
                + ReflectedJavaTypeDescriptors.renderJavaType(listenerMethod.getReturnType()) + ">";
    }

    private static String renderTupleType(Class<?>[] parameterTypes) {
        if (parameterTypes.length == 0) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < parameterTypes.length; index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(ReflectedJavaTypeDescriptors.renderJavaType(parameterTypes[index]));
        }
        return builder.append(']').toString();
    }

    private static final class FabricTypeNode {
        private final String interfaceName;
        private final Map<String, FabricTypeNode> childrenByName = new TreeMap<>();
        private final Map<String, String> eventsByName = new TreeMap<>();

        private FabricTypeNode(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        private FabricTypeNode getOrCreateChild(String memberName) {
            return childrenByName.computeIfAbsent(memberName,
                    ignored -> new FabricTypeNode(buildChildInterfaceName(memberName)));
        }

        private void register(List<String> pathSegments, String fieldName, String eventType) {
            FabricTypeNode current = this;
            for (String pathSegment : pathSegments) {
                current = current.getOrCreateChild(pathSegment);
            }
            current.eventsByName.put(fieldName, eventType);
        }

        private String buildChildInterfaceName(String memberName) {
            StringBuilder builder = new StringBuilder(interfaceName).append('_');
            boolean upperCaseNext = true;

            for (int index = 0; index < memberName.length(); index++) {
                char character = memberName.charAt(index);
                if (!Character.isLetterOrDigit(character)) {
                    upperCaseNext = true;
                    continue;
                }
                if (builder.charAt(builder.length() - 1) == '_' && !Character.isLetter(character) && character != '_') {
                    builder.append('_');
                }
                builder.append(upperCaseNext ? Character.toUpperCase(character) : character);
                upperCaseNext = false;
            }

            if (!TypingsNamingUtils.isValidIdentifier(builder.toString())) {
                return interfaceName + "_Node" + Integer.toUnsignedString(memberName.hashCode());
            }
            return builder.toString();
        }
    }
}
