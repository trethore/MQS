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

package net.me.hooking.context;

import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.HostAccess;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@SuppressWarnings("unused")
public class MethodInfo {
    private final Method method;
    private final MappingsManager mappingsManager;
    private final ScriptManager scriptManager;

    public MethodInfo(Method method, MappingsManager mappingsManager, ScriptManager scriptManager) {
        this.method = method;
        this.mappingsManager = mappingsManager;
        this.scriptManager = scriptManager;
    }

    @HostAccess.Export
    public String getName() {
        return method.getName();
    }

    @HostAccess.Export
    public Object getReturnType() {
        return ScriptUtils.wrapReturn(method.getReturnType(), mappingsManager, scriptManager);
    }

    @HostAccess.Export
    public Object[] getParameterTypes() {
        Class<?>[] params = method.getParameterTypes();
        Object[] wrappedParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            wrappedParams[i] = ScriptUtils.wrapReturn(params[i], mappingsManager, scriptManager);
        }
        return wrappedParams;
    }

    @HostAccess.Export
    public boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }
}