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

package net.me.console;

import lombok.Getter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record ConsoleMessage(String text, MessageType type, String timestamp) {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ConsoleMessage(String text, MessageType type) {
        this(text, type, LocalTime.now().format(TIME_FORMATTER));
    }

    @Getter
    public enum MessageType {
        INFO(0xFFFFFFFF),
        ERROR(0xFFF38BA8),
        COMMAND(0xFF89B4FA),
        SUCCESS(0xFFA6E3A1);

        private final int color;

        MessageType(int color) {
            this.color = color;
        }

    }
}