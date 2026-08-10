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

import io.github.trethore.myqolpackages.internal.network.http.PackageHttpHeader;
import io.github.trethore.myqolpackages.internal.network.http.PackageHttpRequest;
import io.github.trethore.myqolpackages.internal.runtime.graal.api.JavaScriptApiBridge;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.graalvm.polyglot.Value;

final class FetchRequestParser {
    private static final String BODY_OPTION = "body";
    private static final String HEADERS_OPTION = "headers";
    private static final String METHOD_OPTION = "method";
    private static final Set<String> SUPPORTED_OPTIONS = Set.of(METHOD_OPTION, HEADERS_OPTION, BODY_OPTION);

    private final JavaScriptApiBridge adapter;

    FetchRequestParser(JavaScriptApiBridge adapter) {
        this.adapter = adapter;
    }

    PackageHttpRequest parse(Value input, Value init) {
        if (input == null || !input.isString()) {
            throw new IllegalArgumentException("fetch URL must be a string");
        }
        if (init == null || !adapter.isObject(init)) {
            throw new IllegalArgumentException("fetch options must be an object");
        }
        validateOptions(init);
        URI uri = URI.create(input.asString());
        String method = parseMethod(init.getMember(METHOD_OPTION));
        List<PackageHttpHeader> headers = parseHeaders(init.getMember(HEADERS_OPTION));
        return createRequest(uri, method, headers, init.getMember(BODY_OPTION));
    }

    private void validateOptions(Value init) {
        Value keys = adapter.ownKeys(init);
        for (long index = 0; index < keys.getArraySize(); index++) {
            String option = keys.getArrayElement(index).asString();
            if (!SUPPORTED_OPTIONS.contains(option)) {
                throw new IllegalArgumentException("Unsupported fetch option: " + option);
            }
        }
    }

    private String parseMethod(Value method) {
        return isUndefined(method) ? "GET" : adapter.stringify(method).toUpperCase(Locale.ROOT);
    }

    private List<PackageHttpHeader> parseHeaders(Value headers) {
        if (isNullish(headers)) {
            return List.of();
        }
        if (adapter.isArray(headers)) {
            List<PackageHttpHeader> result = new ArrayList<>();
            for (long index = 0; index < headers.getArraySize(); index++) {
                Value entry = headers.getArrayElement(index);
                if (!adapter.isArray(entry) || entry.getArraySize() != 2) {
                    throw new IllegalArgumentException("Invalid header entry");
                }
                result.add(createHeader(entry.getArrayElement(0), entry.getArrayElement(1)));
            }
            return List.copyOf(result);
        }
        if (adapter.isObject(headers)) {
            List<PackageHttpHeader> result = new ArrayList<>();
            Value keys = adapter.ownKeys(headers);
            for (long index = 0; index < keys.getArraySize(); index++) {
                String name = keys.getArrayElement(index).asString();
                result.add(new PackageHttpHeader(name, adapter.stringify(headers.getMember(name))));
            }
            return List.copyOf(result);
        }
        throw new IllegalArgumentException("Headers must be an object or an array");
    }

    private PackageHttpHeader createHeader(Value name, Value value) {
        return new PackageHttpHeader(adapter.stringify(name), adapter.stringify(value));
    }

    private PackageHttpRequest createRequest(URI uri, String method, List<PackageHttpHeader> headers, Value body) {
        if (isNullish(body)) {
            return PackageHttpRequest.withoutBody(uri, method, headers);
        }
        byte[] bodyBytes = adapter.stringify(body).getBytes(StandardCharsets.UTF_8);
        return PackageHttpRequest.withBody(uri, method, headers, bodyBytes);
    }

    private boolean isNullish(Value value) {
        return value == null || value.isNull() || adapter.isUndefined(value);
    }

    private boolean isUndefined(Value value) {
        return value == null || adapter.isUndefined(value);
    }
}
