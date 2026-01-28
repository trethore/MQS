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

package net.me.scripting.api.internal;

import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.utils.ScriptUtils;
import net.me.scripting.wrappers.JsClassWrapper;
import net.me.scripting.wrappers.LazyJsClassHolder;

public final class ClassResolverHelper {
    private final ScriptingClassResolver classResolver;

    public ClassResolverHelper(ScriptingClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    public Class<?> resolveFromValue(Object unwrapped) {
        Object value = ScriptUtils.unwrapReceiver(unwrapped);
        if (value instanceof JsClassWrapper wrapper) {
            return wrapper.getTargetClass();
        }
        if (value instanceof LazyJsClassHolder holder) {
            return holder.getWrapper().getTargetClass();
        }
        if (value instanceof Class<?> cls) {
            return cls;
        }
        throw new IllegalArgumentException("Target must be a class imported via importClass().");
    }

    public HookTarget parseDescriptor(String descriptor) {
        int idx = descriptor.indexOf('#');
        if (idx <= 0 || idx == descriptor.length() - 1) {
            throw new IllegalArgumentException("Descriptor must follow the 'fully.qualified.Class#methodName' format.");
        }
        String className = descriptor.substring(0, idx);
        String methodName = descriptor.substring(idx + 1);
        return new HookTarget(resolveClassName(className), methodName);
    }

    public Class<?> resolveClassName(String className) {
        try {
            String runtimeName = classResolver.getRuntimeName(className);
            String lookup = runtimeName != null ? runtimeName : className;
            return Class.forName(lookup, false, ClassResolverHelper.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not resolve class '" + className + "'.", e);
        }
    }

    public record HookTarget(Class<?> targetClass, String methodName) {
    }
}
