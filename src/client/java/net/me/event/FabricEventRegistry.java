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

package net.me.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class FabricEventRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricEventRegistry.class);
    private static final String FABRIC_API_PACKAGE = "net.fabricmc.fabric.api";
    private static final String FABRIC_API_RESOURCE_PATH = "net/fabricmc/fabric/api";

    private final FabricEventNode root = new FabricEventNode();

    public static List<FabricEventEntry> discoverEntries() {
        List<FabricEventEntry> entries = new ArrayList<>();
        Set<String> classNames = new TreeSet<>();

        for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
            for (Path rootPath : modContainer.getRootPaths()) {
                collectFabricApiClasses(rootPath, classNames);
            }
        }

        for (String className : classNames) {
            registerEventFields(className, entries);
        }

        return List.copyOf(entries);
    }

    private static void collectFabricApiClasses(Path rootPath, Set<String> classNames) {
        Path fabricApiRoot = rootPath.resolve(FABRIC_API_RESOURCE_PATH);
        if (!Files.exists(fabricApiRoot)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(fabricApiRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(FabricEventRegistry::isClassFile)
                    .map(rootPath::relativize)
                    .map(FabricEventRegistry::toClassName)
                    .forEach(classNames::add);
        } catch (Exception exception) {
            LOGGER.debug("Failed to scan Fabric API classes from root {}", rootPath, exception);
        }
    }

    private static boolean isClassFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".class")
                && !fileName.equals("package-info.class")
                && !fileName.equals("module-info.class");
    }

    private static String toClassName(Path relativePath) {
        String classFile = relativePath.toString().replace('\\', '.').replace('/', '.');
        return classFile.substring(0, classFile.length() - ".class".length());
    }

    private static void registerEventFields(String className, List<FabricEventEntry> entries) {
        try {
            Class<?> clazz = Class.forName(className, false, FabricEventRegistry.class.getClassLoader());
            for (Field field : clazz.getFields()) {
                int modifiers = field.getModifiers();
                boolean isEventField = field.getDeclaringClass() == clazz
                        && Modifier.isPublic(modifiers)
                        && Modifier.isStatic(modifiers)
                        && Event.class.isAssignableFrom(field.getType());
                if (isEventField && field.get(null) instanceof Event<?> event) {
                    Class<?> listenerType = FabricEventIntrospection.findListenerType(event);
                    Method listenerMethod = FabricEventIntrospection.findSingleAbstractMethod(listenerType);
                    entries.add(new FabricEventEntry(resolvePathSegments(clazz.getName()), field.getName(), event, listenerType, listenerMethod));
                }
            }
        } catch (Exception exception) {
            LOGGER.debug("Skipping Fabric API class {} during event discovery", className, exception);
        }
    }

    private static List<String> resolvePathSegments(String className) {
        if (!className.startsWith(FABRIC_API_PACKAGE + ".")) {
            return List.of();
        }

        String relativeClassName = className.substring(FABRIC_API_PACKAGE.length() + 1);
        return List.of(relativeClassName.split("\\."));
    }

    public ProxyObject createProxy() {
        return SharedProxyHolder.PROXY;
    }

    private ProxyObject buildProxy() {
        for (FabricEventEntry entry : discoverEntries()) {
            addEvent(entry.pathSegments(), entry.fieldName(), entry.event());
        }
        return new FabricEventNodeProxy(root);
    }

    private void addEvent(List<String> pathSegments, String fieldName, Event<?> event) {
        FabricEventNode current = root;
        for (String pathSegment : pathSegments) {
            current = current.getOrCreateChild(pathSegment);
        }

        current.addEvent(fieldName, event);
    }

    private static final class FabricEventNode {
        private final Map<String, Object> children = new TreeMap<>();

        private FabricEventNode getOrCreateChild(String key) {
            Object existing = children.get(key);
            if (existing instanceof FabricEventNode child) {
                return child;
            }
            if (existing != null) {
                throw new IllegalStateException("Fabric event tree path collision for key '" + key + "'.");
            }

            FabricEventNode child = new FabricEventNode();
            children.put(key, child);
            return child;
        }

        private void addEvent(String key, Event<?> event) {
            Object existing = children.putIfAbsent(key, event);
            if (existing != null && existing != event) {
                throw new IllegalStateException("Duplicate Fabric event entry for key '" + key + "'.");
            }
        }
    }

    private record FabricEventNodeProxy(FabricEventNode node) implements ProxyObject {
        @Override
        public Object getMember(String key) {
            Object member = node.children.get(key);
            if (member instanceof FabricEventNode child) {
                return new FabricEventNodeProxy(child);
            }
            return member;
        }

        @Override
        public Object getMemberKeys() {
            return node.children.keySet().toArray(String[]::new);
        }

        @Override
        public boolean hasMember(String key) {
            return node.children.containsKey(key);
        }

        @Override
        public void putMember(String key, Value value) {
            throw new UnsupportedOperationException("Cannot modify MQS.events.fabric.");
        }
    }

    private static final class SharedProxyHolder {
        private static final ProxyObject PROXY = new FabricEventRegistry().buildProxy();
    }

    public record FabricEventEntry(
            List<String> pathSegments,
            String fieldName,
            Event<?> event,
            Class<?> listenerType,
            Method listenerMethod
    ) {
        public FabricEventEntry {
            pathSegments = List.copyOf(pathSegments);
        }
    }
}
