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
    private static final String KEY_PROP = "key";
    private static final String REPEATABLE_PROP = "repeatable";
    private static final String DEBOUNCE_PROP = "debounce";

    private final int keyCode;
    private final boolean repeatable;
    private final int debounceMillis;

    private KeybindOptions(int keyCode, boolean repeatable, int debounceMillis) {
        this.keyCode = keyCode;
        this.repeatable = repeatable;
        this.debounceMillis = debounceMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static KeybindOptions fromScript(Value optionsValue, int defaultKeyCode) {
        if (optionsValue == null) {
            return builder().keyCode(defaultKeyCode).build();
        }
        KeybindOptions hostOptions = fromHostObject(optionsValue, defaultKeyCode);
        if (hostOptions != null) {
            return hostOptions;
        }

        int keyCode = defaultKeyCode;
        boolean repeatable = false;
        int debounceMillis = 100;

        if (!optionsValue.hasMembers()) {
            return new KeybindOptions(keyCode, repeatable, debounceMillis);
        }

        keyCode = readIntMember(optionsValue, KEY_PROP, keyCode);
        repeatable = readBooleanMember(optionsValue, REPEATABLE_PROP, repeatable);
        debounceMillis = readNonNegativeIntMember(optionsValue, DEBOUNCE_PROP, debounceMillis);

        return new KeybindOptions(keyCode, repeatable, debounceMillis);
    }

    private static KeybindOptions fromHostObject(Value optionsValue, int defaultKeyCode) {
        if (!optionsValue.isHostObject()) {
            return null;
        }
        Object hostObject = optionsValue.asHostObject();
        if (hostObject instanceof KeybindOptions options) {
            return options.withKeyCode(defaultKeyCode);
        }
        if (hostObject instanceof KeybindOptions.Builder builder) {
            return builder.buildWithDefaultKey(defaultKeyCode);
        }
        return null;
    }

    private static int readIntMember(Value optionsValue, String member, int defaultValue) {
        if (!optionsValue.hasMember(member)) {
            return defaultValue;
        }
        Value memberValue = optionsValue.getMember(member);
        if (!memberValue.isNumber()) {
            return defaultValue;
        }
        return memberValue.asInt();
    }

    private static boolean readBooleanMember(Value optionsValue, String member, boolean defaultValue) {
        if (!optionsValue.hasMember(member)) {
            return defaultValue;
        }
        Value memberValue = optionsValue.getMember(member);
        if (!memberValue.isBoolean()) {
            return defaultValue;
        }
        return memberValue.asBoolean();
    }

    private static int readNonNegativeIntMember(Value optionsValue, String member, int defaultValue) {
        int value = readIntMember(optionsValue, member, defaultValue);
        return Math.max(0, value);
    }

    public int keyCode() {
        return keyCode;
    }

    public boolean repeatable() {
        return repeatable;
    }

    public int debounceMillis() {
        return debounceMillis;
    }

    public KeybindOptions withKeyCode(int keyCode) {
        if (this.keyCode == keyCode) {
            return this;
        }
        return new KeybindOptions(keyCode, repeatable, debounceMillis);
    }

    public static final class Builder {
        private int keyCode = Keys.UNBOUND.getCode();
        private boolean repeatable;
        private int debounceMillis = 100;

        @HostAccess.Export
        public Builder key(int keyCode) {
            this.keyCode = keyCode;
            return this;
        }

        public Builder keyCode(int keyCode) {
            return key(keyCode);
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
            return new KeybindOptions(keyCode, repeatable, debounceMillis);
        }

        public KeybindOptions buildWithDefaultKey(int defaultKeyCode) {
            if (keyCode == Keys.UNBOUND.getCode()) {
                return build().withKeyCode(defaultKeyCode);
            }
            return build();
        }
    }
}
