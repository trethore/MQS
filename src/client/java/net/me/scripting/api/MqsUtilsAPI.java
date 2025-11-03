/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
 * Copyright (C) 2025 tytoo
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

package net.me.scripting.api;

import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.me.utils.*;
import net.me.utils.math.*;
import net.minecraft.client.MinecraftClient;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MqsUtilsAPI implements ProxyObject {

    private static final Set<String> SPECIAL_KEYS = Set.of("math", "mc", "schedule");

    private final ScriptingClassResolver classResolver;
    private final ScriptManager scriptManager;
    private final ScriptScheduler scheduler;
    private final Map<String, Class<?>> classExports = new HashMap<>();
    private final ProxyObject mathProxy;
    private final ProxyObject mcProxy;
    private final ProxyObject scheduleProxy;

    public MqsUtilsAPI(ScriptingClassResolver classResolver, ScriptManager scriptManager, ScriptScheduler scheduler) {
        this.classResolver = classResolver;
        this.scriptManager = scriptManager;
        this.scheduler = scheduler;

        classExports.put("render2D", Render2DUtils.class);
        classExports.put("render3D", Render3DUtils.class);
        classExports.put("textRender", TextRenderUtils.class);
        classExports.put("textRenderer", TextRendererUtils.class);
        classExports.put("chat", ChatUtils.class);
        classExports.put("color", ColorUtils.class);
        classExports.put("camera", CameraUtils.class);
        classExports.put("assets", AssetIdentifiers.class);

        this.mathProxy = createMathProxy();
        this.mcProxy = createMcProxy();
        this.scheduleProxy = createScheduleProxy();
    }

    @Override
    public Object getMember(String key) {
        if ("math".equals(key)) {
            return mathProxy;
        }
        if ("mc".equals(key)) {
            return mcProxy;
        }
        if ("schedule".equals(key)) {
            return scheduleProxy;
        }
        Class<?> utilClass = classExports.get(key);
        if (utilClass != null) {
            return classResolver.getOrCreateWrapper(utilClass.getName());
        }
        return null;
    }

    @Override
    public Object getMemberKeys() {
        String[] keys = new String[classExports.size() + SPECIAL_KEYS.size()];
        int idx = 0;
        for (String key : classExports.keySet()) {
            keys[idx++] = key;
        }
        for (String specialKey : SPECIAL_KEYS) {
            keys[idx++] = specialKey;
        }
        return keys;
    }

    @Override
    public boolean hasMember(String key) {
        return classExports.containsKey(key) || SPECIAL_KEYS.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the MQS.utils object.");
    }

    private ProxyObject createMathProxy() {
        Map<String, Class<?>> mathClasses = Map.of(
                "Vector3f", Vector3f.class,
                "Box6f", Box6f.class,
                "Box6d", Box6d.class,
                "Boxable", Boxable.class,
                "Position", Position.class
        );

        return new ProxyObject() {
            @Override
            public Object getMember(String key) {
                Class<?> type = mathClasses.get(key);
                if (type == null) {
                    return null;
                }
                return classResolver.getOrCreateWrapper(type.getName());
            }

            @Override
            public Object getMemberKeys() {
                return mathClasses.keySet().toArray(new String[0]);
            }

            @Override
            public boolean hasMember(String key) {
                return mathClasses.containsKey(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify MQS.utils.math.");
            }
        };
    }

    private ProxyObject createMcProxy() {
        return new ProxyObject() {
            private final Set<String> keys = Set.of("raw", "getMc", "getPlayer", "getWorld", "client", "player", "world", "runOnClientThread");

            @Override
            public Object getMember(String key) {
                return switch (key) {
                    case "raw" -> classResolver.getOrCreateWrapper(McUtils.class.getName());
                    case "getMc" -> (ProxyExecutable) args -> McUtils.getMc();
                    case "getPlayer" -> (ProxyExecutable) args -> McUtils.getPlayer();
                    case "getWorld" -> (ProxyExecutable) args -> McUtils.getWorld();
                    case "client" -> (ProxyExecutable) args -> ScriptUtils.wrapReturn(
                            MinecraftClient.getInstance(),
                            classResolver.getMappingsManager(),
                            scriptManager
                    );
                    case "player" -> (ProxyExecutable) args -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        return client != null
                                ? ScriptUtils.wrapReturn(client.player, classResolver.getMappingsManager(), scriptManager)
                                : null;
                    };
                    case "world" -> (ProxyExecutable) args -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        return client != null
                                ? ScriptUtils.wrapReturn(client.world, classResolver.getMappingsManager(), scriptManager)
                                : null;
                    };
                    case "runOnClientThread" -> (ProxyExecutable) args -> {
                        if (args.length != 1 || args[0] == null || !args[0].canExecute()) {
                            throw new IllegalArgumentException("runOnClientThread requires a callback function.");
                        }
                        RunningScript owner = currentScript();
                        Value callback = args[0];
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.send(() -> executeCallback(owner, callback));
                        return null;
                    };
                    default -> null;
                };
            }

            @Override
            public Object getMemberKeys() {
                return keys.toArray(new String[0]);
            }

            @Override
            public boolean hasMember(String key) {
                return keys.contains(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify MQS.utils.mc.");
            }
        };
    }

    private ProxyObject createScheduleProxy() {
        return new ProxyObject() {
            private final Set<String> keys = Set.of("tickTimeout", "msTimeout", "tickInterval");

            @Override
            public Object getMember(String key) {
                return switch (key) {
                    case "tickTimeout" -> (ProxyExecutable) args -> {
                        ensureCallback(args);
                        RunningScript owner = currentScript();
                        Value callback = args[0];
                        int delay = args.length > 1 ? Math.max(0, args[1].asInt()) : 0;
                        Runnable cancel = scheduler.scheduleTickTimeout(owner, callback, delay);
                        return toDisposer(owner, cancel);
                    };
                    case "msTimeout" -> (ProxyExecutable) args -> {
                        ensureCallback(args);
                        RunningScript owner = currentScript();
                        Value callback = args[0];
                        long delay = args.length > 1 ? Math.max(0L, args[1].asLong()) : 0L;
                        Runnable cancel = scheduler.scheduleMsTimeout(owner, callback, delay);
                        return toDisposer(owner, cancel);
                    };
                    case "tickInterval" -> (ProxyExecutable) args -> {
                        ensureCallback(args);
                        if (args.length < 2 || !args[1].isNumber()) {
                            throw new IllegalArgumentException("tickInterval requires an interval in ticks.");
                        }
                        RunningScript owner = currentScript();
                        Value callback = args[0];
                        int interval = Math.max(1, args[1].asInt());
                        Runnable cancel = scheduler.scheduleTickInterval(owner, callback, interval);
                        return toDisposer(owner, cancel);
                    };
                    default -> null;
                };
            }

            @Override
            public Object getMemberKeys() {
                return keys.toArray(new String[0]);
            }

            @Override
            public boolean hasMember(String key) {
                return keys.contains(key);
            }

            @Override
            public void putMember(String key, Value value) {
                throw new UnsupportedOperationException("Cannot modify MQS.utils.schedule.");
            }
        };
    }

    private void ensureCallback(Value[] args) {
        if (args.length == 0 || args[0] == null || !args[0].canExecute()) {
            throw new IllegalArgumentException("First argument must be a callback function.");
        }
        if (args.length > 1 && args[1] != null && !args[1].isNumber()) {
            throw new IllegalArgumentException("Delay must be numeric.");
        }
    }

    private RunningScript currentScript() {
        RunningScript script = scriptManager.getCurrentScript();
        if (script == null) {
            throw new IllegalStateException("MQS.utils helpers must be invoked from an active script.");
        }
        return script;
    }

    private Value toDisposer(RunningScript owner, Runnable cancel) {
        ProxyExecutable exec = disposeArgs -> {
            cancel.run();
            return null;
        };
        return owner.getContext().asValue(exec);
    }

    private void executeCallback(RunningScript owner, Value callback) {
        RunningScript previous = scriptManager.getCurrentScript();
        scriptManager.setCurrentScript(owner);
        try {
            callback.execute();
        } catch (IllegalStateException ignored) {
        } catch (Exception e) {
            Main.LOGGER.error("runOnClientThread callback threw for script '{}'", owner.getName(), e);
        } finally {
            if (previous != null) {
                scriptManager.setCurrentScript(previous);
            } else {
                scriptManager.clearCurrentScript();
            }
        }
    }
}
