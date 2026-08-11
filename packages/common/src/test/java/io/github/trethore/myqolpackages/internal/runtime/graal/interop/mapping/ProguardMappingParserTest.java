/*
 * My QOL Packages - Client-side Minecraft modding at runtime
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
package io.github.trethore.myqolpackages.internal.runtime.graal.interop.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProguardMappingParserTest {
    @Test
    void parsesClassesAndBuildsCatalogIndexes() throws IOException {
        ProguardMappingParser.ParsedMappings mappings = new ProguardMappingParser().parse(new StringReader("""
                    # metadata
                    net.minecraft.network.chat.Component -> abc:
                        java.lang.String value -> a
                        10:12:java.lang.String render(int) -> b
                        13:13:java.lang.String render(java.lang.String) -> c
                        14:14:void <init>() -> <init>
                    net.minecraft.client.OptionInstance$UnitDouble -> def:
                    """));

        assertEquals("abc", mappings.mappings().getRuntimeClassName("net.minecraft.network.chat.Component"));
        MappingIndex.ClassMapping componentMapping =
                mappings.mappings().getClassMapping("net.minecraft.network.chat.Component");
        assertEquals("a", componentMapping.fieldMappings().get("value"));
        assertEquals(
                List.of(
                        new MappingIndex.MethodMapping("b", List.of("int")),
                        new MappingIndex.MethodMapping("c", List.of("java.lang.String"))),
                componentMapping.methodMappings().get("render"));
        assertFalse(componentMapping.methodMappings().containsKey("<init>"));
        assertTrue(mappings.catalog().containsPackage("net.minecraft.network.chat"));
        assertEquals(
                List.of("net.minecraft.network.chat.Component"),
                mappings.catalog().findBySuffix("chat.Component"));
        assertEquals(
                List.of("net.minecraft.client.OptionInstance$UnitDouble"),
                mappings.catalog().findBySuffix("UnitDouble"));
    }

    @Test
    void rejectsMalformedClassMappings() {
        ProguardMappingParser parser = new ProguardMappingParser();
        StringReader source = new StringReader("invalid mapping");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(source));
    }
}
