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

import net.me.scripting.typings.schema.TsGlobalConstant;
import net.me.scripting.typings.schema.TsGlobalFunction;
import net.me.scripting.typings.schema.TsObject;
import net.me.scripting.typings.schema.TsTypeAlias;

import java.util.ArrayList;
import java.util.List;

public record MqsApiFragment(
        List<TsTypeAlias> typeAliases,
        List<TsGlobalFunction> globalFunctions,
        List<TsGlobalConstant> globalConstants,
        List<TsObject> objects
) {
    public MqsApiFragment {
        typeAliases = List.copyOf(typeAliases);
        globalFunctions = List.copyOf(globalFunctions);
        globalConstants = List.copyOf(globalConstants);
        objects = List.copyOf(objects);
    }

    public static MqsApiFragment empty() {
        return new MqsApiFragment(List.of(), List.of(), List.of(), List.of());
    }

    public static MqsApiFragment merge(MqsApiFragment... fragments) {
        List<TsTypeAlias> typeAliases = new ArrayList<>();
        List<TsGlobalFunction> globalFunctions = new ArrayList<>();
        List<TsGlobalConstant> globalConstants = new ArrayList<>();
        List<TsObject> objects = new ArrayList<>();

        for (MqsApiFragment fragment : fragments) {
            if (fragment == null) {
                continue;
            }
            typeAliases.addAll(fragment.typeAliases());
            globalFunctions.addAll(fragment.globalFunctions());
            globalConstants.addAll(fragment.globalConstants());
            objects.addAll(fragment.objects());
        }

        return new MqsApiFragment(typeAliases, globalFunctions, globalConstants, objects);
    }
}
