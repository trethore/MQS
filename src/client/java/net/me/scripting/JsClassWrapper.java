package net.me.scripting;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

public class JsClassWrapper implements ProxyObject {
    private final Class<?> offCls;
    private final Map<String, List<String>> methodLookup;
    private final Map<String, String>       fieldLookup;

    public JsClassWrapper(String offFqcn,
                          Map<String, List<String>> methodLookup,
                          Map<String, String>       fieldLookup
    ) throws ClassNotFoundException {
        System.out.println("JsClassWrapper: " + offFqcn);
        this.offCls       = Class.forName(offFqcn);
        this.methodLookup = methodLookup;
        this.fieldLookup  = fieldLookup;
    }

    @Override
    public Object getMember(String key) {
        // 1) constructor → wrap in JsObjectWrapper
        if ("new".equals(key)) {
            return (ProxyExecutable) args -> {
                try {
                    for (Constructor<?> ctor : offCls.getConstructors()) {
                        if (ctor.getParameterCount() == args.length) {
                            Object[] javaArgs = ScriptManager.unwrapArguments(args, ctor.getParameterTypes());
                            Object inst = ctor.newInstance(javaArgs);
                            return ScriptManager.wrapReturnValue(inst);
                        }
                    }
                    throw new RuntimeException(
                            "No ctor for " + offCls.getName() +
                                    " with " + args.length + " args");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }

        // 2) static methods only
        if (methodLookup.containsKey(key)) {
            List<String> obfNames = methodLookup.get(key);
            return (ProxyExecutable) args -> {
                try {
                    for (String obf : obfNames) {
                        for (Method m : offCls.getMethods()) {
                            if (m.getName().equals(obf)
                                    && Modifier.isStatic(m.getModifiers())
                                    && m.getParameterCount() == args.length) {
                                Object[] javaArgs = ScriptManager.unwrapArguments(args, m.getParameterTypes());
                                Object result = m.invoke(null, javaArgs);
                                return ScriptManager.wrapReturnValue(result);
                            }
                        }
                    }
                    throw new RuntimeException(
                            "No static-overload of " + key +
                                    " with " + args.length + " args");
                } catch (InvocationTargetException ite) {
                    throw new RuntimeException(ite.getCause());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }

        // 3) static fields
        if (fieldLookup.containsKey(key)) {
            try {
                Field f = offCls.getField(fieldLookup.get(key));
                return f.get(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }
    @Override
    public boolean hasMember(String key) {
        return "new".equals(key)
                || methodLookup.containsKey(key)
                || fieldLookup.containsKey(key);
    }

    @Override
    public Object getMemberKeys() {
        return Stream.concat(
                Stream.of("new"),
                Stream.concat(
                        methodLookup.keySet().stream(),
                        fieldLookup.keySet().stream()
                )
        ).toArray(String[]::new);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Read-only");
    }
}
