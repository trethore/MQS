package net.me.scripting;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

public class JsObjectWrapper implements ProxyObject {
    private final Object instance;
    private final Class<?> offCls;
    private final Map<String,List<String>> methodLookup;
    private final Map<String,String>       fieldLookup;

    public JsObjectWrapper(Object instance,
                           Class<?> offCls,
                           Map<String,List<String>> methodLookup,
                           Map<String,String> fieldLookup) {
        this.instance     = instance;
        this.offCls       = offCls;
        this.methodLookup = methodLookup;
        this.fieldLookup  = fieldLookup;
    }

    public Object getJavaInstance() {
        return this.instance;
    }

    @Override
    public Object getMember(String key) {
        // methods (all instance; static handled in JsClassWrapper)
        if (methodLookup.containsKey(key)) {
            List<String> obfNames = methodLookup.get(key);
            return (ProxyExecutable) args -> {
                try {
                    for (String obf : obfNames) {
                        for (Method m : offCls.getMethods()) {
                            if (m.getName().equals(obf)
                                    && !Modifier.isStatic(m.getModifiers())
                                    && m.getParameterCount() == args.length) {
                               Object[] javaArgs = ScriptManager.unwrapArguments(args, m.getParameterTypes());
                               Object result = m.invoke(instance, javaArgs);
                               return ScriptManager.wrapReturnValue(result);
                           }
                       }
                    }
                    throw new RuntimeException(
                            "No instance-overload of " + key +
                                    " with " + args.length + " args");
                } catch (InvocationTargetException ite) {
                    throw new RuntimeException(ite.getCause());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }

        // fields
        if (fieldLookup.containsKey(key)) {
            try {
                Field f = offCls.getField(fieldLookup.get(key));
                return f.get(instance);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    @Override public boolean hasMember(String key) {
        return methodLookup.containsKey(key)
                || fieldLookup.containsKey(key);
    }

    @Override
    public Object getMemberKeys() {
        return Stream.concat(
                methodLookup.keySet().stream(),
                fieldLookup.keySet().stream()
        ).toArray(String[]::new);
    }

    @Override public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Read-only");
    }
}