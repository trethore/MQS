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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.myqolpackages.internal.runtime.graal.js.JavaScriptModuleLoader;
import io.github.trethore.myqolpackages.internal.runtime.graal.js.JavaScriptRuntimeSupport;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

class PackageApiBuilderTest {
    @Test
    void installsAndExtendsFrozenGuestObjects() {
        PackageApiBuilder builder = new PackageApiBuilder();
        ApiObjectBuilder mqp = builder.defineObjectGlobal("mqp");
        mqp.define("version", "test");
        mqp.define("__proto__", "ordinary-property");
        ApiObjectBuilder example = mqp.defineObject("example");
        example.define("first", "one");
        builder.objectGlobal("mqp").object("example").define("second", "two");
        builder.defineGlobal("exampleGlobal", "global");

        try (Context context = createContext()) {
            JavaScriptRuntimeSupport runtime = createRuntime(context);
            builder.install(runtime.api());

            assertEquals("test", context.eval("js", "mqp.version").asString());
            assertEquals("one", context.eval("js", "mqp.example.first").asString());
            assertEquals("two", context.eval("js", "mqp.example.second").asString());
            assertEquals("global", context.eval("js", "exampleGlobal").asString());
            assertEquals(
                    "ordinary-property", context.eval("js", "mqp.__proto__").asString());
            assertTrue(context.eval("js", "Object.hasOwn(mqp, '__proto__')").asBoolean());
            assertTrue(context.eval("js", "Object.isFrozen(mqp)").asBoolean());
            assertTrue(context.eval("js", "Object.isFrozen(mqp.example)").asBoolean());
            assertTrue(context.eval("js", "Object.getPrototypeOf(mqp) === Object.prototype")
                    .asBoolean());
            assertTrue(context.eval("js", "Object.getPrototypeOf(mqp.example) === Object.prototype")
                    .asBoolean());
        }
    }

    @Test
    void rejectsDuplicateAndLeafObjectConflictsWithCompletePaths() {
        PackageApiBuilder builder = new PackageApiBuilder();
        ApiObjectBuilder mqp = builder.defineObjectGlobal("mqp");
        ApiObjectBuilder nested = mqp.defineObject("nested");
        nested.define("value", "first");

        IllegalArgumentException duplicateLeaf =
                assertThrows(IllegalArgumentException.class, () -> nested.define("value", "second"));
        IllegalArgumentException objectOverLeaf =
                assertThrows(IllegalArgumentException.class, () -> nested.defineObject("value"));
        IllegalArgumentException leafOverObject =
                assertThrows(IllegalArgumentException.class, () -> mqp.define("nested", "value"));
        IllegalArgumentException duplicateGlobal =
                assertThrows(IllegalArgumentException.class, () -> builder.defineGlobal("mqp", "value"));

        assertEquals("Duplicate package API member: mqp.nested.value", duplicateLeaf.getMessage());
        assertEquals("Duplicate package API member: mqp.nested.value", objectOverLeaf.getMessage());
        assertEquals("Duplicate package API member: mqp.nested", leafOverObject.getMessage());
        assertEquals("Duplicate package API member: mqp", duplicateGlobal.getMessage());
    }

    @Test
    void rejectsMissingAndLeafObjectRetrieval() {
        PackageApiBuilder builder = new PackageApiBuilder();
        ApiObjectBuilder mqp = builder.defineObjectGlobal("mqp");
        mqp.define("java", "leaf");

        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> mqp.object("events"));
        IllegalArgumentException leaf = assertThrows(IllegalArgumentException.class, () -> mqp.object("java"));

        assertEquals("Package API node does not exist: mqp.events", missing.getMessage());
        assertEquals("Package API node is not an object: mqp.java", leaf.getMessage());
    }

    @Test
    void rejectsInvalidNamesAndValues() {
        PackageApiBuilder builder = new PackageApiBuilder();
        ApiObjectBuilder mqp = builder.defineObjectGlobal("mqp");

        assertThrows(NullPointerException.class, () -> builder.defineGlobal(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> builder.defineGlobal(" ", "value"));
        assertThrows(NullPointerException.class, () -> builder.defineGlobal("value", null));
        assertThrows(NullPointerException.class, () -> mqp.define(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> mqp.define(" ", "value"));
        assertThrows(NullPointerException.class, () -> mqp.define("value", null));
    }

    @Test
    void rejectsInstallationAndMutationAfterFinalization() {
        PackageApiBuilder builder = new PackageApiBuilder();
        ApiObjectBuilder mqp = builder.defineObjectGlobal("mqp");
        ApiObjectBuilder nested = mqp.defineObject("nested");

        try (Context context = createContext()) {
            JavaScriptRuntimeSupport runtime = createRuntime(context);
            builder.install(runtime.api());

            assertThrows(IllegalStateException.class, () -> builder.install(runtime.api()));
            assertThrows(IllegalStateException.class, () -> builder.defineGlobal("other", "value"));
            assertThrows(IllegalStateException.class, () -> mqp.define("other", "value"));
            assertThrows(IllegalStateException.class, () -> nested.defineObject("other"));
        }
    }

    @Test
    void checksEveryGlobalConflictBeforeInstallingAnyGlobal() {
        PackageApiBuilder builder = new PackageApiBuilder();
        builder.defineGlobal("available", "available-value");
        builder.defineGlobal("occupied", "occupied-value");

        try (Context context = createContext()) {
            JavaScriptRuntimeSupport runtime = createRuntime(context);
            context.eval("js", "Object.defineProperty(globalThis, 'occupied', { value: true })");

            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> builder.install(runtime.api()));

            assertEquals(
                    "Cannot install package API global because globalThis already contains: occupied",
                    exception.getMessage());
            assertFalse(
                    context.eval("js", "Object.hasOwn(globalThis, 'available')").asBoolean());
            assertThrows(IllegalStateException.class, () -> builder.defineGlobal("later", "value"));
        }
    }

    private static Context createContext() {
        return Context.newBuilder("js")
                .option("js.esm-eval-returns-exports", "true")
                .build();
    }

    private static JavaScriptRuntimeSupport createRuntime(Context context) {
        return new JavaScriptRuntimeSupport(new JavaScriptModuleLoader(context));
    }
}
