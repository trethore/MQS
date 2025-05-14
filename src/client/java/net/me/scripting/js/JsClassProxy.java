package net.me.scripting.js;

// ... imports from JsNamespace, ScriptManager, MappingData, org.graalvm.polyglot.* ...
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import net.me.scripting.MappingData;
import net.me.scripting.ScriptManager;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

public class JsClassProxy implements ProxyObject {
    private final String yarnClassName;
    private final Class<?> officialClass;
    private final MappingData mappingData;
    private final ScriptManager scriptManager;

    public JsClassProxy(String yarnClassName, MappingData mappingData, ScriptManager scriptManager) {
        this.yarnClassName = yarnClassName;
        this.mappingData = mappingData;
        this.scriptManager = scriptManager;
        this.officialClass = mappingData.getOfficialClassName(yarnClassName)
                .flatMap(name -> {
                    try {
                        return Optional.of(Class.forName(name, true, scriptManager.getClass().getClassLoader()));
                    } catch (ClassNotFoundException e) {
                        scriptManager.getLogger().warn("Official class not found for {}: {}", yarnClassName, name);
                        return Optional.empty();
                    }
                })
                .orElse(null);

        if (this.officialClass == null) {
            scriptManager.getLogger().error("Failed to resolve official class for Yarn name: {}", yarnClassName);
        }
    }

    public Class<?> getOfficialClass() { return officialClass; }


    @Override
    public Object getMember(String key) {
        if (officialClass == null) throw new RuntimeException("Class not loaded: " + yarnClassName);

        if ("new".equals(key)) {
            return new JsConstructorProxy(officialClass, mappingData, scriptManager);
        }

        // Static Field
        Optional<String> officialFieldName = mappingData.getOfficialFieldName(yarnClassName, key);
        if (officialFieldName.isPresent()) {
            try {
                Field field = officialClass.getField(officialFieldName.get());
                if (Modifier.isStatic(field.getModifiers())) {
                    return JsUtils.javaToGraal(field.get(null), scriptManager);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) { /* try method */ }
        }

        // Static Method (simplified lookup - no full overload resolution here)
        // Find methods in officialClass that could match the yarn name 'key'
        // This requires iterating through officialClass.getMethods() and trying to map their names/descriptors back to yarn
        // Or, iterate MappingData's methods for this class.
        final MappingTree.ClassMapping classMappingView = mappingData.getMappingTree().getClass(yarnClassName.replace('.','/'), mappingData.getNamedNsId());
        if (classMappingView != null) {
            for(MappingTree.MethodMapping methodView : classMappingView.getMethods()) {
                if (key.equals(methodView.getName(mappingData.getNamedNsId()))) {
                    String officialMethodName = methodView.getName(mappingData.getOfficialNsId());
                    String officialMethodDesc = mappingData.mapDescriptorToOfficial(methodView.getDesc(mappingData.getNamedNsId()));

                    // This is a ProxyExecutable representing the group of overloaded methods
                    return (ProxyExecutable) args -> {
                        // Find the best matching static method on officialClass
                        // This is the most complex part: overload resolution
                        Method bestMatch = findMatchingMethod(officialClass, officialMethodName, args, true, scriptManager);
                        if (bestMatch != null) {
                            try {
                                Object[] javaArgs = JsUtils.graalToJavaArgs(args, bestMatch.getParameterTypes(), scriptManager);
                                Object result = bestMatch.invoke(null, javaArgs);
                                return JsUtils.javaToGraal(result, scriptManager);
                            } catch (Exception e) {
                                throw new RuntimeException("Error invoking static method " + yarnClassName + "." + key, e);
                            }
                        }
                        throw new NoSuchMethodError("No matching static method " + key + " for arguments " + Arrays.toString(args) + " on " + yarnClassName);
                    };
                }
            }
        }
        scriptManager.getLogger().warn("Static member {} not found in class {}", key, yarnClassName);
        return null;
    }

    // Simplified method finder (needs proper type matching and scoring for overloads)
    public static Method findMatchingMethod(Class<?> clazz, String name, Value[] jsArgs, boolean isStatic, ScriptManager sm) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name) && Modifier.isStatic(method.getModifiers()) == isStatic && method.getParameterCount() == jsArgs.length) {
                // Further checks: convert jsArgs to tentative Java types and check assignability
                // This is where true overload resolution logic would go.
                // For now, first match by name, static-ness, and param count.
                return method;
            }
        }
        return null;
    }


    @Override
    public Object getMemberKeys() { /* ... (list static members) ... */ return new String[0]; }
    @Override
    public boolean hasMember(String key) { /* ... (check static members) ... */ return true; }
    @Override
    public void putMember(String key, Value value) { /* Static fields could be settable */ }
}