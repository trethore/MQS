package net.me.scripting.js;

// ... imports ...
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import net.me.scripting.MappingData;
import net.me.scripting.ScriptManager;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

public class JsInstanceProxy implements ProxyObject {
    private final Object javaInstance;
    private final String instanceYarnClassName; // Yarn name of the actual instance's class
    private final MappingData mappingData;
    private final ScriptManager scriptManager;

    public JsInstanceProxy(Object javaInstance, String instanceYarnClassName, MappingData mappingData, ScriptManager scriptManager) {
        this.javaInstance = javaInstance;
        this.instanceYarnClassName = instanceYarnClassName;
        this.mappingData = mappingData;
        this.scriptManager = scriptManager;
    }

    public Object getJavaInstance() {
        return javaInstance;
    }

    @Override
    public Object getMember(String key) {
        Class<?> actualClass = javaInstance.getClass();

        // Field
        Optional<String> officialFieldName = mappingData.getOfficialFieldName(instanceYarnClassName, key);
        if (officialFieldName.isPresent()) {
            try {
                Field field = actualClass.getField(officialFieldName.get());
                if (!Modifier.isStatic(field.getModifiers())) {
                    return JsUtils.javaToGraal(field.get(javaInstance), scriptManager);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) { /* try method */ }
        }

        // Method
        final MappingTree.ClassMapping classMappingView = mappingData.getMappingTree().getClass(instanceYarnClassName.replace('.','/'), mappingData.getNamedNsId());
        if (classMappingView != null) {
            for(MappingTree.MethodMapping methodView : classMappingView.getMethods()) {
                 if (key.equals(methodView.getName(mappingData.getNamedNsId()))) {
                    String officialMethodName = methodView.getName(mappingData.getOfficialNsId());
                    // String officialMethodDesc = mappingData.mapDescriptorToOfficial(methodView.getDesc(mappingData.getNamedNsId())); // For precise overload matching

                    return (ProxyExecutable) args -> {
                        Method bestMatch = JsClassProxy.findMatchingMethod(actualClass, officialMethodName, args, false, scriptManager);
                         if (bestMatch != null) {
                            try {
                                Object[] javaArgs = JsUtils.graalToJavaArgs(args, bestMatch.getParameterTypes(), scriptManager);
                                Object result = bestMatch.invoke(javaInstance, javaArgs);
                                return JsUtils.javaToGraal(result, scriptManager);
                            } catch (Exception e) {
                                throw new RuntimeException("Error invoking method " + instanceYarnClassName + "#" + key, e);
                            }
                        }
                        throw new NoSuchMethodError("No matching method " + key + " for arguments on instance of " + instanceYarnClassName);
                    };
                }
            }
        }
        scriptManager.getLogger().warn("Instance member {} not found in class {}", key, instanceYarnClassName);
        return null;
    }

    @Override
    public void putMember(String key, Value value) {
        Optional<String> officialFieldName = mappingData.getOfficialFieldName(instanceYarnClassName, key);
        if (officialFieldName.isPresent()) {
            try {
                Field field = javaInstance.getClass().getField(officialFieldName.get());
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.set(javaInstance, JsUtils.graalToJava(value, field.getType(), scriptManager));
                    return;
                }
            } catch (Exception e) {
                scriptManager.getLogger().error("Failed to set field " + key + " on " + instanceYarnClassName, e);
            }
        }
        throw new UnsupportedOperationException("Cannot set member " + key + " on " + instanceYarnClassName);
    }


    @Override
    public Object getMemberKeys() { /* ... */ return new String[0]; }
    @Override
    public boolean hasMember(String key) { /* ... */ return true; }
}