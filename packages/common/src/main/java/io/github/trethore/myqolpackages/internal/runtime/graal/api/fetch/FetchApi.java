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

import io.github.trethore.myqolpackages.internal.runtime.graal.http.PackageHttpClient;
import io.github.trethore.myqolpackages.internal.runtime.graal.http.PackageHttpRequest;
import io.github.trethore.myqolpackages.internal.runtime.graal.http.PackageHttpResponse;
import io.github.trethore.myqolpackages.internal.runtime.graal.js.JavaScriptValueSupport;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

final class FetchApi implements ProxyExecutable, AutoCloseable {
    private static final int MAX_COMPLETIONS_PER_TICK = 64;

    private final ConcurrentLinkedQueue<Runnable> completions = new ConcurrentLinkedQueue<>();
    private final PackageHttpClient httpClient;
    private final FetchRequestParser requestParser;

    private volatile boolean closed;

    FetchApi(JavaScriptValueSupport javaScriptValues, PackageHttpClient httpClient) {
        this.requestParser = new FetchRequestParser(Objects.requireNonNull(javaScriptValues, "javaScriptValues"));
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public Object execute(Value... arguments) {
        if (arguments.length != 4 || !arguments[2].canExecute() || !arguments[3].canExecute()) {
            throw new IllegalArgumentException("Invalid fetch bridge invocation");
        }
        if (closed) {
            throw new IllegalStateException("Fetch API is closed");
        }
        PackageHttpRequest request = requestParser.parse(arguments[0], arguments[1]);
        Value resolve = arguments[2];
        Value reject = arguments[3];
        httpClient
                .send(request)
                .whenComplete((response, throwable) -> enqueueCompletion(resolve, reject, response, throwable));
        return true;
    }

    public void tick() {
        for (int count = 0; count < MAX_COMPLETIONS_PER_TICK; count++) {
            Runnable completion = completions.poll();
            if (completion == null) {
                return;
            }
            completion.run();
        }
    }

    @Override
    public void close() {
        closed = true;
        completions.clear();
    }

    private void enqueueCompletion(Value resolve, Value reject, PackageHttpResponse response, Throwable throwable) {
        if (closed) {
            return;
        }
        Runnable completion = () -> {
            if (closed) {
                return;
            }
            if (throwable == null) {
                resolve.execute(new FetchResponse(response));
            } else {
                reject.execute(errorMessage(throwable));
            }
        };
        completions.add(completion);
        if (closed) {
            completions.remove(completion);
        }
    }

    private static String errorMessage(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause.getClass() == RuntimeException.class)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
