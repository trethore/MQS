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
package io.github.trethore.myqolpackages.internal.runtime.graal.api;

import io.github.trethore.myqolpackages.internal.runtime.graal.js.JavaScriptApiSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PackageApiBuilder {
    private final BuildState state = new BuildState();
    private final ObjectNode root = new ObjectNode("", state);

    public void defineGlobal(String name, Object value) {
        root.define(name, value);
    }

    public ApiObjectBuilder defineObjectGlobal(String name) {
        return new ApiObjectBuilder(root.defineObject(name));
    }

    public ApiObjectBuilder objectGlobal(String name) {
        return new ApiObjectBuilder(root.object(name));
    }

    void install(JavaScriptApiSupport javaScriptApi) {
        Objects.requireNonNull(javaScriptApi, "javaScriptApi");
        state.finalizeTree();
        for (Map.Entry<String, Node> entry : root.children()) {
            String name = entry.getKey();
            if (javaScriptApi.hasOwnGlobal(name)) {
                throw new IllegalStateException(
                        "Cannot install package API global because globalThis already contains: " + name);
            }
        }
        Map<String, Object> globals = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : root.children()) {
            globals.put(entry.getKey(), materialize(entry.getValue(), javaScriptApi));
        }
        globals.forEach(javaScriptApi::defineGlobal);
    }

    private static Object materialize(Node node, JavaScriptApiSupport javaScriptApi) {
        if (node instanceof ValueNode(Object value)) {
            return value;
        }
        ObjectNode objectNode = (ObjectNode) node;
        Map<String, Object> members = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : objectNode.children()) {
            members.put(entry.getKey(), materialize(entry.getValue(), javaScriptApi));
        }
        return javaScriptApi.createFrozenObject(members);
    }

    private sealed interface Node permits ObjectNode, ValueNode {}

    static final class ObjectNode implements Node {
        private final Map<String, Node> children = new LinkedHashMap<>();
        private final String path;
        private final BuildState state;

        private ObjectNode(String path, BuildState state) {
            this.path = path;
            this.state = state;
        }

        void define(String name, Object value) {
            state.requireBuilding();
            String childPath = childPath(name);
            Objects.requireNonNull(value, "value");
            requireAvailable(name, childPath);
            children.put(name, new ValueNode(value));
        }

        ObjectNode defineObject(String name) {
            state.requireBuilding();
            String childPath = childPath(name);
            requireAvailable(name, childPath);
            ObjectNode child = new ObjectNode(childPath, state);
            children.put(name, child);
            return child;
        }

        ObjectNode object(String name) {
            String childPath = childPath(name);
            Node child = children.get(name);
            if (child == null) {
                throw new IllegalArgumentException("Package API node does not exist: " + childPath);
            }
            if (!(child instanceof ObjectNode objectNode)) {
                throw new IllegalArgumentException("Package API node is not an object: " + childPath);
            }
            return objectNode;
        }

        private Iterable<Map.Entry<String, Node>> children() {
            return children.entrySet();
        }

        private String childPath(String name) {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Package API member name must not be blank");
            }
            if (path.isEmpty()) {
                return name;
            }
            return path + "." + name;
        }

        private void requireAvailable(String name, String childPath) {
            if (children.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate package API member: " + childPath);
            }
        }
    }

    private record ValueNode(Object value) implements Node {}

    private static final class BuildState {
        private boolean finalized;

        private void finalizeTree() {
            requireBuilding();
            finalized = true;
        }

        private void requireBuilding() {
            if (finalized) {
                throw new IllegalStateException("Package API tree has already been finalized");
            }
        }
    }
}
