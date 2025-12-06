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

package net.me.scripting.extenders.proxies;

import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.utils.MappingUtils;
import net.me.scripting.wrappers.JsObjectWrapper;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class MappedInstanceProxy implements ProxyObject {
    private final Object extendedInstance;
    private final JsObjectWrapper methodAndFieldWrapper;

    public MappedInstanceProxy(Object extendedInstance, ScriptingClassResolver resolver) {
        this.extendedInstance = extendedInstance;

        MappingUtils.ClassMappings cm = MappingUtils.combineMappings(
                extendedInstance.getClass(),
                resolver.getRuntimeToYarnMap(),
                resolver.getMethodMap(),
                resolver.getFieldMap()
        );

        this.methodAndFieldWrapper = new JsObjectWrapper(
                extendedInstance,
                extendedInstance.getClass(),
                cm.methods(),
                cm.fields(),
                resolver.getMappingsManager(),
                resolver.getScriptManager()
        );
    }

    @Override
    public Object getMember(String key) {
        if ("_self".equals(key)) {
            return extendedInstance;
        }
        return methodAndFieldWrapper.getMember(key);
    }

    @Override
    public Object getMemberKeys() {
        return methodAndFieldWrapper.getMemberKeys();
    }

    @Override
    public boolean hasMember(String key) {
        return methodAndFieldWrapper.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        methodAndFieldWrapper.putMember(key, value);
    }

    public Object getInstance() {
        return extendedInstance;
    }
}
