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
package io.github.trethore.myqolpackages.internal.runtime.graal;

public final class GeneratedTypeFixture {
    private static Object storedValue;

    private GeneratedTypeFixture() {}

    public static Object getStoredValue() {
        return storedValue;
    }

    public static void setStoredValue(Object value) {
        storedValue = value;
    }

    public abstract static class AbstractBase {
        public abstract int compute(int value);
    }

    public static class Base {
        public String value;

        public Base() {
            value = "empty";
        }

        public Base(int amount) {
            value = "number:" + amount;
        }

        public Base(String name) {
            value = "string:" + name;
        }

        public String callsVirtual(String name) {
            return greet(name);
        }

        public String greet(String name) {
            return "base:" + name;
        }
    }

    public interface Left {
        default String choose() {
            return "left";
        }
    }

    public static class EarlyBase {
        public final String observed;

        public EarlyBase() {
            observed = describe();
        }

        public String describe() {
            return "base";
        }
    }

    public interface ParentDefault {
        default String label() {
            return "parent";
        }
    }

    public interface Right {
        default String choose() {
            return "right";
        }
    }
}
