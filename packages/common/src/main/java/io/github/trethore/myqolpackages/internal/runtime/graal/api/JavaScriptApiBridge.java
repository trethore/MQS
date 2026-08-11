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
package io.github.trethore.myqolpackages.internal.runtime.graal.api;

import java.util.Objects;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public final class JavaScriptApiBridge {
    private static final Source SOURCE = PackageApiSourceLoader.load();

    private final Value createFetch;
    private final Value createJavaApi;
    private final Value createMqp;
    private final Value defineGlobal;
    private final Value isArray;
    private final Value isObject;
    private final Value isUndefined;
    private final Value ownKeys;
    private final Value stringify;

    public JavaScriptApiBridge(Context context) {
        Value factory = Objects.requireNonNull(context, "context").eval(SOURCE).getMember("default");
        if (factory == null || !factory.canExecute()) {
            throw new IllegalStateException(
                    "JavaScript API resource must have a default function export: " + SOURCE.getName());
        }
        Value adapter = factory.execute();
        this.createFetch = requireFunction(adapter, "createFetch");
        this.createJavaApi = requireFunction(adapter, "createJavaApi");
        this.createMqp = requireFunction(adapter, "createMqp");
        this.defineGlobal = requireFunction(adapter, "defineGlobal");
        this.isArray = requireFunction(adapter, "isArray");
        this.isObject = requireFunction(adapter, "isObject");
        this.isUndefined = requireFunction(adapter, "isUndefined");
        this.ownKeys = requireFunction(adapter, "ownKeys");
        this.stringify = requireFunction(adapter, "stringify");
    }

    public void defineGlobal(String name, Object value) {
        defineGlobal.execute(name, value);
    }

    public Value createFetch(ProxyExecutable fetchBridge) {
        return createFetch.execute(fetchBridge);
    }

    public Value createJavaApi(
            Class<?> voidType,
            Class<?> booleanType,
            Class<?> byteType,
            Class<?> shortType,
            Class<?> intType,
            Class<?> longType,
            Class<?> floatType,
            Class<?> doubleType,
            Class<?> charType) {
        return createJavaApi.execute(
                voidType, booleanType, byteType, shortType, intType, longType, floatType, doubleType, charType);
    }

    public Value createMqp(String version, String dataDirectory, String packageId, Value javaApi) {
        return createMqp.execute(version, dataDirectory, packageId, javaApi);
    }

    public boolean isArray(Value value) {
        return isArray.execute(value).asBoolean();
    }

    public boolean isObject(Value value) {
        return isObject.execute(value).asBoolean();
    }

    public boolean isUndefined(Value value) {
        return isUndefined.execute(value).asBoolean();
    }

    public Value ownKeys(Value value) {
        return ownKeys.execute(value);
    }

    public String stringify(Value value) {
        return stringify.execute(value).asString();
    }

    private static Value requireFunction(Value adapter, String member) {
        Value function = adapter.getMember(member);
        if (function == null || !function.canExecute()) {
            throw new IllegalStateException("Missing JavaScript runtime adapter function: " + member);
        }
        return function;
    }
}
