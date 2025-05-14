package net.me.scripting.js;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

// Helper interfaces for conciseness
public class JsProxy {
    @FunctionalInterface
    public interface Function extends ProxyExecutable {
        @Override
        default Object execute(Value... arguments) {
            return apply(arguments);
        }
        Object apply(Value... arguments);
    }

    public interface ObjectProxy extends ProxyObject {
        @Override
        default void putMember(String key, Value value) {
            throw new UnsupportedOperationException("Read-only proxy object");
        }
    }
}