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

package net.me.scripting.typings;

import net.me.scripting.typings.schema.*;

import java.util.List;

/**
 * Responsible for generating TypeScript declaration content for the My QOL Scripts API from shared runtime descriptors.
 */
final class MqsApiDtsEmitter {
    private static final String INDENT = TypingsFormat.INDENT;
    private static final String READONLY = TypingsFormat.READONLY;
    private static final String BLOCK_END = TypingsFormat.BLOCK_END;

    public void append(StringBuilder builder) {
        append(builder, MqsApiTypeDescriptors.describe());
    }

    public void append(StringBuilder builder, MqsApiFragment fragment) {
        appendTypeAliases(builder, fragment.typeAliases());
        appendGlobalFunctions(builder, fragment.globalFunctions());
        appendGlobalConstants(builder, fragment.globalConstants());
        appendSchemaObjects(builder, fragment.objects());
    }

    private void appendTypeAliases(StringBuilder builder, List<TsTypeAlias> typeAliases) {
        for (TsTypeAlias typeAlias : typeAliases) {
            builder.append("type ").append(typeAlias.name());
            if (typeAlias.typeParameters() != null) {
                builder.append(typeAlias.typeParameters());
            }
            builder.append(" = ").append(typeAlias.definition()).append(";\n");
        }
        if (!typeAliases.isEmpty()) {
            builder.append("\n");
        }
    }

    private void appendGlobalFunctions(StringBuilder builder, List<TsGlobalFunction> globalFunctions) {
        for (TsGlobalFunction globalFunction : globalFunctions) {
            for (TsFunction overload : globalFunction.overloads()) {
                builder.append("declare function ")
                        .append(globalFunction.name())
                        .append(renderTypeParameters(overload))
                        .append("(")
                        .append(renderParams(overload.params()))
                        .append("): ")
                        .append(overload.returnType())
                        .append(";\n");
            }
        }
        if (!globalFunctions.isEmpty()) {
            builder.append("\n");
        }
    }

    private void appendGlobalConstants(StringBuilder builder, List<TsGlobalConstant> globalConstants) {
        for (TsGlobalConstant globalConstant : globalConstants) {
            builder.append("declare const ")
                    .append(globalConstant.name())
                    .append(": ")
                    .append(globalConstant.type())
                    .append(";\n");
        }
        if (!globalConstants.isEmpty()) {
            builder.append("\n");
        }
    }

    private void appendSchemaObjects(StringBuilder builder, List<TsObject> objects) {
        for (TsObject object : objects) {
            appendObject(builder, object);
        }
    }

    private void appendObject(StringBuilder builder, TsObject object) {
        builder.append("interface ").append(object.name()).append(" {\n");
        for (TsMember member : object.members()) {
            switch (member) {
                case TsProperty property -> appendProperty(builder, property);
                case TsMethod method -> appendMethod(builder, method);
            }
        }
        builder.append(BLOCK_END);
    }

    private void appendProperty(StringBuilder builder, TsProperty property) {
        String memberName = renderMemberName(property.name());
        builder.append(INDENT);
        if (property.readOnly()) {
            builder.append(READONLY);
        }
        builder.append(memberName);
        if (property.optional() && !isIndexSignature(memberName)) {
            builder.append("?");
        }
        builder.append(": ").append(property.type()).append(";\n");
    }

    private void appendMethod(StringBuilder builder, TsMethod method) {
        for (TsFunction overload : method.overloads()) {
            builder.append(INDENT)
                    .append(renderMemberName(method.name()))
                    .append(renderTypeParameters(overload))
                    .append("(")
                    .append(renderParams(overload.params()))
                    .append("): ")
                    .append(overload.returnType())
                    .append(";\n");
        }
    }

    private String renderParams(List<TsParam> params) {
        if (params.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < params.size(); index++) {
            TsParam parameter = params.get(index);
            if (index > 0) {
                builder.append(", ");
            }
            if (parameter.rest()) {
                builder.append("...");
            }
            builder.append(parameter.name());
            if (parameter.optional() && !parameter.rest()) {
                builder.append("?");
            }
            builder.append(": ");
            if (parameter.rest()) {
                builder.append("Array<").append(parameter.type()).append(">");
            } else {
                builder.append(parameter.type());
            }
        }
        return builder.toString();
    }

    private String renderTypeParameters(TsFunction function) {
        return function.typeParameters() != null ? function.typeParameters() : "";
    }

    private String renderMemberName(String memberName) {
        if (isIndexSignature(memberName)) {
            return memberName;
        }
        if (TypingsDeclarationUtils.isAccessibleIdentifier(memberName)) {
            return memberName;
        }
        return "'" + TypingsNamingUtils.escapeSingleQuotedString(memberName) + "'";
    }

    private boolean isIndexSignature(String memberName) {
        return memberName.startsWith("[") && memberName.endsWith("]");
    }
}
