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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop.generation;

import java.util.HashMap;
import java.util.Map;

final class GeneratedTypeRegistry {
    static final GeneratedTypeRegistry INSTANCE = new GeneratedTypeRegistry();

    private final Map<RegistryKey, RegistryEntry> entries = new HashMap<>();

    private GeneratedTypeRegistry() {}

    synchronized Class<?> defineOrReuse(
            ClassLoader classLoader,
            GeneratedTypeDefinition definition,
            GeneratedCallbackBinding binding,
            TypeLoader loader) {
        RegistryKey key = new RegistryKey(classLoader, definition.binaryName());
        RegistryEntry existing = entries.get(key);
        if (existing != null) {
            if (!existing.packageId.equals(definition.packageId())) {
                throw new IllegalStateException(
                        "Generated type " + definition.binaryName() + " is owned by package " + existing.packageId);
            }
            if (existing.binding.belongsTo(binding.session())) {
                throw new IllegalStateException(
                        "Generated type was already defined by this package context: " + definition.binaryName());
            }
            if (!existing.signature.equals(definition.structuralSignature())) {
                throw new IllegalStateException("Generated type "
                        + definition.binaryName()
                        + " was already loaded with a different structure; restart the game to redefine it");
            }
            binding.bindClass(existing.loadedClass);
            existing.binding = binding;
            return existing.loadedClass;
        }

        Class<?> alreadyLoaded = loadIfPresent(definition.binaryName(), classLoader);
        if (alreadyLoaded != null) {
            throw new IllegalStateException(
                    "A JVM type with binary name " + alreadyLoaded.getName() + " is already loaded");
        }

        RegistryEntry entry =
                new RegistryEntry(definition.packageId(), definition.structuralSignature(), binding, null);
        entries.put(key, entry);
        try {
            Class<?> loadedClass = loader.load();
            binding.bindClass(loadedClass);
            entry.loadedClass = loadedClass;
            return loadedClass;
        } catch (RuntimeException | LinkageError exception) {
            entries.remove(key);
            binding.close();
            throw exception;
        }
    }

    private static Class<?> loadIfPresent(String binaryName, ClassLoader classLoader) {
        try {
            return Class.forName(binaryName, false, classLoader);
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    synchronized GeneratedCallbackBinding getBinding(Class<?> generatedClass) {
        RegistryEntry entry = entries.get(new RegistryKey(generatedClass.getClassLoader(), generatedClass.getName()));
        if (entry == null || (entry.loadedClass != null && entry.loadedClass != generatedClass)) {
            throw new IllegalStateException("No generated callback binding for " + generatedClass.getName());
        }
        return entry.binding;
    }

    private static final class RegistryEntry {
        private GeneratedCallbackBinding binding;
        private Class<?> loadedClass;
        private final String packageId;
        private final GeneratedTypeDefinition.StructuralSignature signature;

        private RegistryEntry(
                String packageId,
                GeneratedTypeDefinition.StructuralSignature signature,
                GeneratedCallbackBinding binding,
                Class<?> loadedClass) {
            this.packageId = packageId;
            this.signature = signature;
            this.binding = binding;
            this.loadedClass = loadedClass;
        }
    }

    // Registry keys must compare class loaders by identity.
    @SuppressWarnings("ClassCanBeRecord")
    private static final class RegistryKey {
        private final ClassLoader classLoader;
        private final String binaryName;

        private RegistryKey(ClassLoader classLoader, String binaryName) {
            this.classLoader = classLoader;
            this.binaryName = binaryName;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof RegistryKey other
                    && classLoader == other.classLoader
                    && binaryName.equals(other.binaryName);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(classLoader) + binaryName.hashCode();
        }
    }

    @FunctionalInterface
    interface TypeLoader {
        Class<?> load();
    }
}
