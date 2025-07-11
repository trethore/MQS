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

package net.me.scripting.api;

import net.me.scripting.engine.ScriptingClassResolver;
import net.me.utils.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

public class MqsUtilsAPI implements ProxyObject {

    private final ScriptingClassResolver classResolver;
    private final Map<String, Class<?>> utilsMap = new HashMap<>();

    public MqsUtilsAPI(ScriptingClassResolver classResolver) {
        this.classResolver = classResolver;

        utilsMap.put("Render2D", Render2DUtils.class);
        utilsMap.put("Render3D", Render3DUtils.class);
        utilsMap.put("TextRender", TextRenderUtils.class);
        utilsMap.put("TextRenderer", TextRendererUtils.class);
        utilsMap.put("Chat", ChatUtils.class);
        utilsMap.put("Color", ColorUtils.class);
        utilsMap.put("Camera", CameraUtils.class);
        utilsMap.put("Mc", McUtils.class);
    }

    @Override
    public Object getMember(String key) {
        Class<?> utilClass = utilsMap.get(key);
        if (utilClass != null) {
            return classResolver.getOrCreateWrapper(utilClass.getName());
        }
        return null;
    }

    @Override
    public Object getMemberKeys() {
        return utilsMap.keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return utilsMap.containsKey(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the MQSUtils object.");
    }
}