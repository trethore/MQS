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
        if (optionsValue.isHostObject()) {
            Object hostObject = optionsValue.asHostObject();
            if (hostObject instanceof KeybindOptions options) {
                return options.withKeyCode(defaultKeyCode);
            }
            if (hostObject instanceof KeybindOptions.Builder builder) {
                return builder.buildWithDefaultKey(defaultKeyCode);
            }
        }

        int keyCode = defaultKeyCode;
        boolean repeatable = false;
        int debounceMillis = 100;

        if (optionsValue.hasMembers()) {
            if (optionsValue.hasMember(KEY_PROP) && optionsValue.getMember(KEY_PROP).isNumber()) {
                keyCode = optionsValue.getMember(KEY_PROP).asInt();
            }
            if (optionsValue.hasMember(REPEATABLE_PROP) && optionsValue.getMember(REPEATABLE_PROP).isBoolean()) {
                repeatable = optionsValue.getMember(REPEATABLE_PROP).asBoolean();
            }
            if (optionsValue.hasMember(DEBOUNCE_PROP) && optionsValue.getMember(DEBOUNCE_PROP).isNumber()) {
                int candidate = optionsValue.getMember(DEBOUNCE_PROP).asInt();
                debounceMillis = Math.max(0, candidate);
            }
        }

        return new KeybindOptions(keyCode, repeatable, debounceMillis);
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
