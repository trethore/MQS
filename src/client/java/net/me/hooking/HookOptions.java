/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

package net.me.hooking;

import org.graalvm.polyglot.Value;

import java.util.Locale;

public final class HookOptions {
    private static final String ARG_COUNT_KEY = "args";
    private static final String MODE_KEY = "mode";

    private final Integer argCount;
    private final HookExecutionMode mode;

    private HookOptions(Integer argCount, HookExecutionMode mode) {
        this.argCount = argCount;
        this.mode = mode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static HookOptions fromScript(Value optionsValue, HookExecutionMode defaultMode) {
        Integer parsedArgCount = extractArgCount(optionsValue);
        HookExecutionMode parsedMode = extractMode(optionsValue, defaultMode);
        return builder()
                .argCount(parsedArgCount)
                .mode(parsedMode)
                .build();
    }

    public static HookOptions withEnforcedMode(Value optionsValue, HookExecutionMode enforcedMode) {
        Integer parsedArgCount = extractArgCount(optionsValue);
        return builder()
                .argCount(parsedArgCount)
                .mode(enforcedMode)
                .build();
    }

    public static Integer extractArgCount(Value optionsValue) {
        if (optionsValue == null || !optionsValue.hasMembers() || !optionsValue.hasMember(ARG_COUNT_KEY)) {
            return null;
        }
        Value member = optionsValue.getMember(ARG_COUNT_KEY);
        if (member != null && member.isNumber()) {
            return member.asInt();
        }
        return null;
    }

    public static HookExecutionMode extractMode(Value optionsValue, HookExecutionMode defaultMode) {
        if (optionsValue == null || !optionsValue.hasMembers() || !optionsValue.hasMember(MODE_KEY)) {
            return defaultMode;
        }

        Value member = optionsValue.getMember(MODE_KEY);
        if (member == null) {
            return defaultMode;
        }

        HookExecutionMode resolved = resolveModeFromValue(member);
        return resolved != null ? resolved : defaultMode;
    }

    private static HookExecutionMode resolveModeFromValue(Value member) {
        if (member.isHostObject() && member.asHostObject() instanceof HookExecutionMode modeValue) {
            return modeValue;
        }

        if (member.isString()) {
            return parseModeString(member.asString());
        }

        return null;
    }

    private static HookExecutionMode parseModeString(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return HookExecutionMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    public Integer argCount() {
        return argCount;
    }

    public HookExecutionMode mode() {
        return mode;
    }

    public static final class Builder {
        private Integer argCount;
        private HookExecutionMode mode;

        private Builder() {
        }

        public Builder argCount(Integer argCount) {
            this.argCount = argCount;
            return this;
        }

        public Builder mode(HookExecutionMode mode) {
            this.mode = mode;
            return this;
        }

        public HookOptions build() {
            return new HookOptions(argCount, mode);
        }
    }
}
