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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

final class FetchHeaders implements ProxyObject {
    private static final String ENTRIES_MEMBER = "entries";
    private static final String GET_MEMBER = "get";
    private static final String HAS_MEMBER = "has";
    private static final String KEYS_MEMBER = "keys";
    private static final String VALUES_MEMBER = "values";
    private static final ProxyArray MEMBER_KEYS =
            ProxyArray.fromArray(GET_MEMBER, HAS_MEMBER, ENTRIES_MEMBER, KEYS_MEMBER, VALUES_MEMBER);

    private final List<Entry> entries;

    FetchHeaders(Map<String, List<String>> headers) {
        List<Entry> normalizedEntries = new ArrayList<>();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            for (String value : header.getValue()) {
                normalizedEntries.add(new Entry(header.getKey().toLowerCase(Locale.ROOT), value));
            }
        }
        this.entries = List.copyOf(normalizedEntries);
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case GET_MEMBER -> (ProxyExecutable) arguments -> get(requireName(key, arguments));
            case HAS_MEMBER -> (ProxyExecutable) arguments -> has(requireName(key, arguments));
            case ENTRIES_MEMBER -> entrySnapshot();
            case KEYS_MEMBER -> stringSnapshot(Entry::name);
            case VALUES_MEMBER -> stringSnapshot(Entry::value);
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
            case GET_MEMBER, HAS_MEMBER, ENTRIES_MEMBER, KEYS_MEMBER, VALUES_MEMBER -> true;
            default -> false;
        };
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Fetch headers are read-only");
    }

    String get(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        StringJoiner values = new StringJoiner(", ");
        boolean found = false;
        for (Entry entry : entries) {
            if (entry.name().equals(normalizedName)) {
                values.add(entry.value());
                found = true;
            }
        }
        return found ? values.toString() : null;
    }

    boolean has(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        for (Entry entry : entries) {
            if (entry.name().equals(normalizedName)) {
                return true;
            }
        }
        return false;
    }

    private ProxyArray entrySnapshot() {
        List<Object> snapshot = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            snapshot.add(ProxyArray.fromArray(entry.name(), entry.value()));
        }
        return ProxyArray.fromList(snapshot);
    }

    private ProxyArray stringSnapshot(Function<Entry, String> valueExtractor) {
        List<Object> snapshot = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            snapshot.add(valueExtractor.apply(entry));
        }
        return ProxyArray.fromList(snapshot);
    }

    private static String requireName(String member, Value[] arguments) {
        if (arguments.length != 1 || !arguments[0].isString()) {
            throw new IllegalArgumentException(member + " requires one string argument");
        }
        return arguments[0].asString();
    }

    private record Entry(String name, String value) {}
}
