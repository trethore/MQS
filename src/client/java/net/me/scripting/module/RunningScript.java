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

package net.me.scripting.module;

import net.me.Main;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class RunningScript {
    private final ScriptDescriptor descriptor;
    private final String name;
    private Value jsInstance;
    private Context context;

    public RunningScript(ScriptDescriptor descriptor, Value jsInstance, Context context) {
        this.descriptor = descriptor;
        this.jsInstance = jsInstance;
        this.context = context;
        this.name = descriptor.moduleName();
    }

    public void onEnable() {
        if (jsInstance.hasMember("onEnable")) {
            try {
                jsInstance.getMember("onEnable").execute();
            } catch (Exception e) {
                Main.LOGGER.error("Error enabling script '{}': {}", name, e.getMessage(), e);
            }
        }
    }

    public void onDisable() {
        if (jsInstance.hasMember("onDisable")) {
            try {
                jsInstance.getMember("onDisable").execute();
            } catch (Exception e) {
                Main.LOGGER.error("Error disabling script '{}': {}", name, e.getMessage(), e);
            }
        }
    }


    public void invalidate() {
        this.context = null;
        this.jsInstance = null;
    }

    private void ensureValid() {
        if (context == null) {
            throw new IllegalStateException("Attempted to use a script that has been disabled and its context recycled.");
        }
    }

    public String getId() {
        return descriptor.getId();
    }

    public Value getJsInstance() {
        ensureValid();
        return jsInstance;
    }

    public Context getContext() {
        ensureValid();
        return context;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return descriptor.version();
    }

    public ScriptDescriptor getDescriptor() {
        return descriptor;
    }
}