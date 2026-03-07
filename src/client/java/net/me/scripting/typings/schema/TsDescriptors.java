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

package net.me.scripting.typings.schema;

import java.util.List;

public final class TsDescriptors {
    private TsDescriptors() {
    }

    public static TsTypeAlias alias(String name, String definition) {
        return new TsTypeAlias(name, definition);
    }

    public static TsTypeAlias alias(String name, String typeParameters, String definition) {
        return new TsTypeAlias(name, definition, typeParameters);
    }

    public static TsGlobalFunction globalFunction(String name, TsFunction... overloads) {
        return new TsGlobalFunction(name, List.of(overloads));
    }

    public static TsGlobalConstant globalConstant(String name, String type) {
        return new TsGlobalConstant(name, type);
    }

    public static TsProperty ro(String name, String type) {
        return new TsProperty(name, type, true, false);
    }

    public static TsProperty prop(String name, String type) {
        return new TsProperty(name, type, false, false);
    }

    public static TsProperty optProp(String name, String type) {
        return new TsProperty(name, type, false, true);
    }

    public static TsMethod method(String name, TsFunction... overloads) {
        return new TsMethod(name, List.of(overloads));
    }

    public static TsFunction fn(String returnType, TsParam... params) {
        return new TsFunction(List.of(params), returnType);
    }

    public static TsFunction fn(String typeParameters, String returnType, TsParam... params) {
        return new TsFunction(List.of(params), returnType, typeParameters);
    }

    public static TsParam p(String name, String type) {
        return new TsParam(name, type, false, false);
    }

    public static TsParam opt(String name, String type) {
        return new TsParam(name, type, true, false);
    }

    public static TsParam rest(String name, String type) {
        return new TsParam(name, type, false, true);
    }
}
