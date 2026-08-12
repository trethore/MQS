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

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.internal.runtime.graal.interop.JavaInteropAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public final class JavaTypeGenerationService implements AutoCloseable {
    private final ClassLoader classLoader;
    private final ProxyExecutable defineClass;
    private final ProxyExecutable defineInterface;
    private final GeneratedTypeEmitter emitter = new GeneratedTypeEmitter();
    private final JavaInteropAccess interop;
    private final GeneratedTypeNameResolver nameResolver = new GeneratedTypeNameResolver();
    private final JavaTypeGenerationSession session;
    private final GeneratedJavaTypeResolver typeResolver;
    private final GeneratedTypeValidator validator;

    public JavaTypeGenerationService(MqpRuntimeEnvironment environment, String packageId, JavaInteropAccess interop) {
        this.classLoader = environment.classLoader();
        this.interop = interop;
        this.session = new JavaTypeGenerationSession(packageId);
        this.typeResolver = new GeneratedJavaTypeResolver(interop);
        this.validator = new GeneratedTypeValidator(interop);
        this.defineClass = arguments -> createBuilder(arguments, GeneratedTypeDefinition.Kind.CLASS);
        this.defineInterface = arguments -> createBuilder(arguments, GeneratedTypeDefinition.Kind.INTERFACE);
    }

    public ProxyExecutable defineClass() {
        return defineClass;
    }

    public ProxyExecutable defineInterface() {
        return defineInterface;
    }

    @Override
    public void close() {
        session.close();
    }

    String packageId() {
        return session.packageId();
    }

    GeneratedJavaTypeResolver typeResolver() {
        return typeResolver;
    }

    Object build(GeneratedTypeDefinition unvalidatedDefinition) {
        GeneratedTypeDefinition definition = validator.validate(unvalidatedDefinition);
        GeneratedCallbackBinding binding = new GeneratedCallbackBinding(definition, interop, session);
        session.addBinding(binding);
        Class<?> generatedClass = GeneratedTypeRegistry.INSTANCE.defineOrReuse(
                classLoader, definition, binding, () -> emitter.emit(definition, classLoader));
        return interop.createClassProxy(definition.binaryName(), generatedClass);
    }

    private Object createBuilder(Value[] arguments, GeneratedTypeDefinition.Kind kind) {
        if (arguments.length != 1 || !arguments[0].isString()) {
            String apiName = kind == GeneratedTypeDefinition.Kind.CLASS ? "defineClass" : "defineInterface";
            throw new IllegalArgumentException(apiName + " requires exactly one type name string");
        }
        String binaryName = nameResolver.resolve(arguments[0].asString(), packageId(), kind);
        return kind == GeneratedTypeDefinition.Kind.CLASS
                ? new JavaClassBuilderProxy(this, binaryName)
                : new JavaInterfaceBuilderProxy(this, binaryName);
    }
}
