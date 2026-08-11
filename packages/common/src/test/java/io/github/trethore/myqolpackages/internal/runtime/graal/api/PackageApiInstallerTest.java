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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.internal.runtime.PackageContextSpec;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackageApiInstallerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void installsTicksAndClosesModulesInLifecycleOrder() {
        List<String> events = new ArrayList<>();
        PackageApiInstaller installer =
                createInstaller(List.of(recordingModule("first", events), recordingModule("second", events)));

        try (Context context = createContext();
                PackageApiSession session = installer.install(context, createSpec())) {
            session.tick();
        }

        assertEquals(
                List.of("install:first", "install:second", "tick:first", "tick:second", "close:second", "close:first"),
                events);
    }

    @Test
    void closesInstalledModulesWhenInstallationFails() {
        List<String> events = new ArrayList<>();
        PackageApiModule failingModule = context -> {
            events.add("install:failing");
            throw new IllegalStateException("failed");
        };
        PackageApiInstaller installer = createInstaller(List.of(recordingModule("first", events), failingModule));
        PackageContextSpec spec = createSpec();

        try (Context context = createContext()) {
            assertThrows(IllegalStateException.class, () -> installer.install(context, spec));
        }

        assertEquals(List.of("install:first", "install:failing", "close:first"), events);
    }

    @Test
    void installsContributedGlobalsAndMqpMembers() {
        PackageApiModule module = context -> {
            context.globals().define("exampleGlobal", "global-value");
            context.mqp().define("example", "mqp-value");
            return PackageApiSession.empty();
        };
        PackageApiInstaller installer = new PackageApiInstaller("1.2.3", List.of(module));

        try (Context context = createContext()) {
            PackageApiSession session = installer.install(context, createSpec());
            try (session) {
                assertEquals("global-value", context.eval("js", "exampleGlobal").asString());
                assertEquals("mqp-value", context.eval("js", "mqp.example").asString());
                assertEquals("1.2.3", context.eval("js", "mqp.version").asString());
                assertTrue(context.eval("js", "Object.isFrozen(mqp)").asBoolean());
            }
        }
    }

    @Test
    void rejectsDuplicateGlobalContributionsAndClosesInstalledModules() {
        List<String> events = new ArrayList<>();
        PackageApiModule first = context -> {
            context.globals().define("duplicate", "first");
            return recordingSession("first", events);
        };
        PackageApiModule second = context -> {
            context.globals().define("duplicate", "second");
            return PackageApiSession.empty();
        };
        PackageApiInstaller installer = createInstaller(List.of(first, second));
        PackageContextSpec spec = createSpec();

        try (Context context = createContext()) {
            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> installer.install(context, spec));

            assertTrue(exception.getMessage().contains("Duplicate package API global"));
        }
        assertEquals(List.of("close:first"), events);
    }

    @Test
    void rejectsDuplicateMqpMembersAndClosesInstalledModules() {
        List<String> events = new ArrayList<>();
        PackageApiModule first = context -> {
            context.mqp().define("duplicate", "first");
            return recordingSession("first", events);
        };
        PackageApiModule second = context -> {
            context.mqp().define("duplicate", "second");
            return PackageApiSession.empty();
        };
        PackageApiInstaller installer = createInstaller(List.of(first, second));
        PackageContextSpec spec = createSpec();

        try (Context context = createContext()) {
            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> installer.install(context, spec));

            assertTrue(exception.getMessage().contains("Duplicate MQP API member"));
        }
        assertEquals(List.of("close:first"), events);
    }

    @Test
    void closesInstalledModulesWhenGlobalFinalizationFails() {
        List<String> events = new ArrayList<>();
        PackageApiModule module = context -> {
            context.globals().define("occupied", "value");
            return recordingSession("module", events);
        };
        PackageApiInstaller installer = createInstaller(List.of(module));
        PackageContextSpec spec = createSpec();

        try (Context context = createContext()) {
            context.eval("js", "Object.defineProperty(globalThis, 'occupied', { value: true })");

            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> installer.install(context, spec));

            assertTrue(exception.getMessage().contains("globalThis already contains it: occupied"));
        }
        assertEquals(List.of("close:module"), events);
    }

    @Test
    void rejectsReservedMqpMembers() {
        PackageApiModule module = context -> {
            context.mqp().define("version", "invalid");
            return PackageApiSession.empty();
        };
        PackageApiInstaller installer = createInstaller(List.of(module));
        PackageContextSpec spec = createSpec();

        try (Context context = createContext()) {
            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> installer.install(context, spec));

            assertTrue(exception.getMessage().contains("member name is reserved"));
        }
    }

    private PackageContextSpec createSpec() {
        return new PackageContextSpec(
                "test-package",
                temporaryDirectory,
                temporaryDirectory.resolve("index.js"),
                temporaryDirectory.resolve("data"));
    }

    private static Context createContext() {
        return Context.newBuilder("js")
                .option("js.esm-eval-returns-exports", "true")
                .build();
    }

    private static PackageApiInstaller createInstaller(List<PackageApiModule> modules) {
        return new PackageApiInstaller("test", modules);
    }

    private static PackageApiModule recordingModule(String name, List<String> events) {
        return context -> {
            events.add("install:" + name);
            return recordingSession(name, events);
        };
    }

    private static PackageApiSession recordingSession(String name, List<String> events) {
        return new PackageApiSession() {
            @Override
            public void tick() {
                events.add("tick:" + name);
            }

            @Override
            public void close() {
                events.add("close:" + name);
            }
        };
    }
}
