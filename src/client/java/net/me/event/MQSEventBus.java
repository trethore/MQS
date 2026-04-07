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

import net.me.Main;

public class MQSEventBus {

    private static EventManager eventManagerInstance;

    private MQSEventBus() {
    }

    public static void setManager(EventManager manager) {
        eventManagerInstance = manager;
    }

    public static void post(Event event) {
        if (eventManagerInstance != null) {
            eventManagerInstance.post(event);
        } else {
            Main.LOGGER.warn("GlobalEventBus.post called before EventManager was set!");
        }
    }
}
