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
package io.github.trethore.myqolpackages.internal.runtime;

@SuppressWarnings({"unused", "FieldMayBeFinal", "java:S1068", "java:S1144"})
public final class MappedClassFixture extends MappedBaseFixture {
  private static final String HIDDEN_STATIC_FINAL_VALUE = "static-final";
  private static String hiddenStaticCollision = "static-field";
  private static String hiddenStaticValue = "initial-static";

  private final String hiddenFinalValue = "instance-final";
  private int hiddenCount;
  private String hiddenName;
  private String hiddenValue = "instance-field";

  private MappedClassFixture(String name, int count) {
    this.hiddenName = name;
    this.hiddenCount = count;
  }

  private static String hiddenChooseNumber(int value) {
    return "number:" + value;
  }

  private static String hiddenChooseString(String value) {
    return "string:" + value;
  }

  private static String hiddenSpecificObject(Object value) {
    return "object:" + value;
  }

  private static String hiddenSpecificString(String value) {
    return "string:" + value;
  }

  private static String hiddenShared(int value) {
    return "shared-number:" + value;
  }

  private static String hiddenShared(String value) {
    return "shared-string:" + value;
  }

  private static String hiddenStaticCollision() {
    return "static-method";
  }

  private static String hiddenStaticGreeting(String name) {
    return "hello " + name;
  }

  private int hiddenIncrement(int amount) {
    hiddenCount += amount;
    return hiddenCount;
  }

  private MappedClassFixture hiddenCopy() {
    return new MappedClassFixture(hiddenName, hiddenCount);
  }

  private String hiddenJoin(String prefix, String... values) {
    return prefix + ":" + String.join(",", values);
  }

  private boolean hiddenSame(MappedClassFixture other) {
    return other == this;
  }

  private String hiddenValue() {
    return "instance-method";
  }
}

@SuppressWarnings({"unused", "FieldMayBeFinal", "java:S1068", "java:S1144"})
class MappedBaseFixture {
  private String hiddenBaseValue = "base-field";

  private String hiddenBaseMethod() {
    return "base-method";
  }
}
