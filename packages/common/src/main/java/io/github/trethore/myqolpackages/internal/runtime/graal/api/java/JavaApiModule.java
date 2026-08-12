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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.java;

import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiInstallContext;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiModule;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.PackageApiSession;
import io.github.trethore.myqolpackages.internal.runtime.graal.interop.ClassInteropBridge;
import io.github.trethore.myqolpackages.internal.runtime.graal.interop.ClassInteropBridgeFactory;
import io.github.trethore.myqolpackages.internal.runtime.graal.interop.generation.JavaTypeGenerationService;

public final class JavaApiModule implements PackageApiModule {
    private final ClassInteropBridgeFactory bridgeFactory;
    private final MqpRuntimeEnvironment environment;

    public JavaApiModule(MqpRuntimeEnvironment environment) {
        this.environment = environment;
        bridgeFactory = new ClassInteropBridgeFactory(environment);
    }

    @Override
    public PackageApiSession install(PackageApiInstallContext context) {
        ClassInteropBridge interopBridge = bridgeFactory.create();
        JavaTypeGenerationService typeGeneration =
                new JavaTypeGenerationService(environment, context.spec().packageId(), interopBridge.interopAccess());
        try {
            context.mqp()
                    .define(
                            "java",
                            JavaApiScript.create(
                                    context.javaScriptModules(),
                                    typeGeneration.defineClass(),
                                    typeGeneration.defineInterface()));
            context.api().defineGlobal("importClass", interopBridge.importClass());
            context.api().defineGlobal("wrap", interopBridge.wrap());
            context.api().defineGlobal("packages", interopBridge.packages());
            context.api().defineGlobal("net", interopBridge.net());
            return new JavaApiSession(typeGeneration);
        } catch (RuntimeException exception) {
            typeGeneration.close();
            throw exception;
        }
    }

    // Sessions have lifecycle and identity semantics, not value semantics.
    @SuppressWarnings("ClassCanBeRecord")
    private static final class JavaApiSession implements PackageApiSession {
        private final JavaTypeGenerationService typeGeneration;

        private JavaApiSession(JavaTypeGenerationService typeGeneration) {
            this.typeGeneration = typeGeneration;
        }

        @Override
        public void close() {
            typeGeneration.close();
        }
    }
}
