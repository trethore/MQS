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

package net.me.scripting.script;

import java.nio.file.Path;
import java.util.Objects;

public record ScriptDescriptor(
        Path path,
        String scriptName,
        String version,
        String mainClass
) {

    public String getId() {
        return path.getFileName().toString() + ":" + scriptName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptDescriptor that = (ScriptDescriptor) o;
        return Objects.equals(path, that.path) && Objects.equals(scriptName, that.scriptName) && Objects.equals(version, that.version) && Objects.equals(mainClass, that.mainClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, scriptName, version, mainClass);
    }
}
