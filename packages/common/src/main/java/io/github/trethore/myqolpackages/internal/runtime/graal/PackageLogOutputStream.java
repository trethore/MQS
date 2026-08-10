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
package io.github.trethore.myqolpackages.internal.runtime.graal;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

final class PackageLogOutputStream extends OutputStream {
    private static final int MAX_LINE_BYTES = 8 * 1024;

    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
    private final Logger logger;
    private final String packageId;
    private final boolean errorOutput;

    private boolean lineTruncated;

    PackageLogOutputStream(Logger logger, String packageId, boolean errorOutput) {
        this.logger = logger;
        this.packageId = packageId;
        this.errorOutput = errorOutput;
    }

    @Override
    public synchronized void write(int value) {
        writeByte(value & 0xFF);
    }

    @Override
    public synchronized void write(byte @NotNull [] bytes, int offset, int length) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.checkFromIndexSize(offset, length, bytes.length);
        for (int index = offset; index < offset + length; index++) {
            writeByte(Byte.toUnsignedInt(bytes[index]));
        }
    }

    private void writeByte(int value) {
        if (value == '\n') {
            writeLine();
            return;
        }
        if (value != '\r') {
            if (lineBuffer.size() < MAX_LINE_BYTES) {
                lineBuffer.write(value);
            } else {
                lineTruncated = true;
            }
        }
    }

    @Override
    public synchronized void flush() {
        writeLine();
    }

    @Override
    public synchronized void close() {
        writeLine();
    }

    private void writeLine() {
        if (lineBuffer.size() == 0) {
            return;
        }
        String line = lineBuffer.toString(StandardCharsets.UTF_8);
        lineBuffer.reset();
        if (lineTruncated) {
            line += " [truncated]";
            lineTruncated = false;
        }
        if (errorOutput) {
            logger.error("[{}] {}", packageId, line);
        } else {
            logger.info("[{}] {}", packageId, line);
        }
    }
}
