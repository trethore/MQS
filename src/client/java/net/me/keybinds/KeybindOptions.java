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

package net.me.keybinds;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

public final class KeybindOptions {
    private static final String REPEATABLE_PROP = "repeatable";
    private static final String DEBOUNCE_PROP = "debounce";

    private final boolean repeatable;
    private final int debounceMillis;

    private KeybindOptions(boolean repeatable, int debounceMillis) {
        this.repeatable = repeatable;
        this.debounceMillis = debounceMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static KeybindOptions fromScript(Value optionsValue) {
        if (optionsValue == null) {
            return builder().build();
        }
        KeybindOptions hostOptions = fromHostObject(optionsValue);
        if (hostOptions != null) {
            return hostOptions;
        }

        boolean repeatable = false;
        int debounceMillis = 100;

        if (!optionsValue.hasMembers()) {
            return new KeybindOptions(repeatable, debounceMillis);
        }

        repeatable = readRepeatableMember(optionsValue, repeatable);
        debounceMillis = readDebounceMember(optionsValue, debounceMillis);

        return new KeybindOptions(repeatable, debounceMillis);
    }

    private static KeybindOptions fromHostObject(Value optionsValue) {
        if (!optionsValue.isHostObject()) {
            return null;
        }
        Object hostObject = optionsValue.asHostObject();
        if (hostObject instanceof KeybindOptions options) {
            return options;
        }
        if (hostObject instanceof KeybindOptions.Builder builder) {
            return builder.build();
        }
        return null;
    }

    private static boolean readRepeatableMember(Value optionsValue, boolean defaultValue) {
        if (!optionsValue.hasMember(REPEATABLE_PROP)) {
            return defaultValue;
        }
        Value memberValue = optionsValue.getMember(REPEATABLE_PROP);
        if (!memberValue.isBoolean()) {
            return defaultValue;
        }
        return memberValue.asBoolean();
    }

    private static int readDebounceMember(Value optionsValue, int defaultValue) {
        if (!optionsValue.hasMember(DEBOUNCE_PROP)) {
            return defaultValue;
        }
        Value memberValue = optionsValue.getMember(DEBOUNCE_PROP);
        if (!memberValue.isNumber()) {
            return defaultValue;
        }
        int value = memberValue.asInt();
        return Math.max(0, value);
    }

    public boolean repeatable() {
        return repeatable;
    }

    public int debounceMillis() {
        return debounceMillis;
    }

    public static final class Builder {
        private boolean repeatable;
        private int debounceMillis = 100;

        @HostAccess.Export
        public Builder repeatable() {
            this.repeatable = true;
            return this;
        }

        @HostAccess.Export
        public Builder repeatable(boolean repeatable) {
            this.repeatable = repeatable;
            return this;
        }

        @HostAccess.Export
        public Builder debounce(Number debounceMillis) {
            if (debounceMillis == null) {
                this.debounceMillis = 100;
            } else {
                this.debounceMillis = Math.max(0, debounceMillis.intValue());
            }
            return this;
        }

        public Builder debounceMillis(int debounceMillis) {
            return debounce(debounceMillis);
        }

        @HostAccess.Export
        public KeybindOptions build() {
            return new KeybindOptions(repeatable, debounceMillis);
        }
    }
}
