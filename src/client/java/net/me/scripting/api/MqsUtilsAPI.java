package net.me.scripting.api;

import net.me.scripting.engine.ScriptingClassResolver;
import net.me.utils.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;

public class MqsUtilsAPI implements ProxyObject {

    private final ScriptingClassResolver classResolver;
    private final Map<String, Class<?>> utilsMap = new HashMap<>();

    public MqsUtilsAPI(ScriptingClassResolver classResolver) {
        this.classResolver = classResolver;

        utilsMap.put("Render2D", Render2DUtils.class);
        utilsMap.put("Render3D", Render3DUtils.class);
        utilsMap.put("TextRender", TextRenderUtils.class);
        utilsMap.put("TextRenderer", TextRendererUtils.class);
        utilsMap.put("Chat", ChatUtils.class);
        utilsMap.put("Color", ColorUtils.class);
        utilsMap.put("Camera", CameraUtils.class);
        utilsMap.put("Mc", McUtils.class);
    }

    @Override
    public Object getMember(String key) {
        Class<?> utilClass = utilsMap.get(key);
        if (utilClass != null) {
            return classResolver.getOrCreateWrapper(utilClass.getName());
        }
        return null;
    }

    @Override
    public Object getMemberKeys() {
        return utilsMap.keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return utilsMap.containsKey(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the MQSUtils object.");
    }
}