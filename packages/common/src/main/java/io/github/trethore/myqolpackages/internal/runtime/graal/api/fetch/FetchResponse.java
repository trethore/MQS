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
package io.github.trethore.myqolpackages.internal.runtime.graal.api.fetch;

import io.github.trethore.myqolpackages.internal.runtime.graal.api.fetch.http.PackageHttpResponse;
import java.nio.charset.StandardCharsets;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

final class FetchResponse implements ProxyObject {
    private static final String BODY_USED_MEMBER = "bodyUsed";
    private static final String CONSUME_BYTES_MEMBER = "consumeBytes";
    private static final String CONSUME_TEXT_MEMBER = "consumeText";
    private static final String HEADERS_MEMBER = "headers";
    private static final String OK_MEMBER = "ok";
    private static final String REDIRECTED_MEMBER = "redirected";
    private static final String STATUS_MEMBER = "status";
    private static final String STATUS_TEXT_MEMBER = "statusText";
    private static final String URL_MEMBER = "url";
    private static final ProxyArray MEMBER_KEYS = ProxyArray.fromArray(
            STATUS_MEMBER,
            STATUS_TEXT_MEMBER,
            OK_MEMBER,
            URL_MEMBER,
            REDIRECTED_MEMBER,
            HEADERS_MEMBER,
            BODY_USED_MEMBER,
            CONSUME_TEXT_MEMBER,
            CONSUME_BYTES_MEMBER);

    private final byte[] body;
    private final FetchHeaders headers;
    private final boolean ok;
    private final boolean redirected;
    private final int status;
    private final String statusText;
    private final String url;

    private boolean bodyUsed;

    FetchResponse(PackageHttpResponse response) {
        this.status = response.status();
        this.statusText = FetchStatus.text(status);
        this.ok = status >= 200 && status <= 299;
        this.url = response.url();
        this.redirected = response.redirected();
        this.headers = new FetchHeaders(response.headers());
        this.body = response.body();
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case STATUS_MEMBER -> status;
            case STATUS_TEXT_MEMBER -> statusText;
            case OK_MEMBER -> ok;
            case URL_MEMBER -> url;
            case REDIRECTED_MEMBER -> redirected;
            case HEADERS_MEMBER -> headers;
            case BODY_USED_MEMBER -> isBodyUsed();
            case CONSUME_TEXT_MEMBER ->
                (ProxyExecutable) arguments -> {
                    requireNoArguments(CONSUME_TEXT_MEMBER, arguments);
                    return consumeText();
                };
            case CONSUME_BYTES_MEMBER ->
                (ProxyExecutable) arguments -> {
                    requireNoArguments(CONSUME_BYTES_MEMBER, arguments);
                    return consumeBytes();
                };
            default -> null;
        };
    }

    @Override
    public Object getMemberKeys() {
        return MEMBER_KEYS;
    }

    @Override
    public boolean hasMember(String key) {
        return switch (key) {
            case STATUS_MEMBER,
                    STATUS_TEXT_MEMBER,
                    OK_MEMBER,
                    URL_MEMBER,
                    REDIRECTED_MEMBER,
                    HEADERS_MEMBER,
                    BODY_USED_MEMBER,
                    CONSUME_TEXT_MEMBER,
                    CONSUME_BYTES_MEMBER -> true;
            default -> false;
        };
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Fetch response is read-only");
    }

    boolean isBodyUsed() {
        return bodyUsed;
    }

    String consumeText() {
        consumeBody();
        return new String(body, StandardCharsets.UTF_8);
    }

    ProxyArray consumeBytes() {
        consumeBody();
        return new UnsignedByteArray(body);
    }

    private void consumeBody() {
        if (bodyUsed) {
            throw new IllegalStateException("Response body has already been used");
        }
        bodyUsed = true;
    }

    private static void requireNoArguments(String member, Value[] arguments) {
        if (arguments.length != 0) {
            throw new IllegalArgumentException(member + " does not accept arguments");
        }
    }

    private static final class UnsignedByteArray implements ProxyArray {
        private final byte[] values;

        private UnsignedByteArray(byte[] values) {
            this.values = values;
        }

        @Override
        public Object get(long index) {
            return Byte.toUnsignedInt(values[Math.toIntExact(index)]);
        }

        @Override
        public void set(long index, Value value) {
            throw new UnsupportedOperationException("Fetch response bytes are read-only");
        }

        @Override
        public long getSize() {
            return values.length;
        }
    }
}
