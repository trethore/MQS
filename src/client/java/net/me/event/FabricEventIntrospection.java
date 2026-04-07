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

package net.me.event;

import net.fabricmc.fabric.api.event.Event;
import net.me.mixin.fabric.event.ArrayBackedEventAccessor;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class FabricEventIntrospection {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricEventIntrospection.class);

    private FabricEventIntrospection() {
    }

    public static Class<?> findListenerType(Event<?> fabricEvent) {
        if (fabricEvent instanceof ArrayBackedEventAccessor<?> accessor) {
            return accessor.getHandlers().getClass().getComponentType();
        }

        LOGGER.warn("Attempting to find listener type for non-ArrayBackedEvent: {}. This may fail.",
                fabricEvent.getClass());

        return Arrays.stream(fabricEvent.getClass().getMethods())
                .filter(method -> method.getName().equals("register")
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] != Identifier.class)
                .findFirst()
                .map(method -> method.getParameterTypes()[0])
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not find register method on event " + fabricEvent));
    }

    public static Method findSingleAbstractMethod(Class<?> listenerType) {
        return Arrays.stream(listenerType.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not find abstract method in " + listenerType.getName()));
    }
}
