/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2026 Titouan Rethore
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

package net.me.scripting.typings;

import net.me.scripting.api.*;
import net.me.scripting.engine.ScriptContextFactory;
import net.me.scripting.engine.ScriptingApi;

public final class MqsApiTypeDescriptors {
    private MqsApiTypeDescriptors() {
    }

    public static MqsApiFragment describe() {
        return MqsApiFragment.merge(
                ScriptingApi.describeTypeScript(),
                EventsAPI.describeTypeScript(),
                ConfigsAPI.describeTypeScript(),
                KeybindsAPI.describeTypeScript(),
                CommandsAPI.describeTypeScript(),
                HooksAPI.describeTypeScript(),
                MqsUtilsAPI.describeTypeScript(),
                ScriptContextFactory.describeTypeScript()
        );
    }
}
