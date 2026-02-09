/*
 * My QOL Scripts - A powerful scripting mod for Minecraft.
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

package net.me.scripting.api;

import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import net.me.utils.*;
import net.me.utils.math.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.me.scripting.api.ApiConstants.*;

public class MqsUtilsAPI implements ProxyObject {
    private static final String MC_RAW = "raw";
    private static final String MC_GET_MC = "getMc";
    private static final String MC_GET_PLAYER = "getPlayer";
    private static final String MC_GET_WORLD = "getWorld";
    private static final String MC_CLIENT = "client";
    private static final String MC_PLAYER = "player";
    private static final String MC_WORLD = "world";
    private static final String MC_RUN_ON_CLIENT_THREAD = "runOnClientThread";
    private static final String SCHEDULE_TICK_TIMEOUT = "tickTimeout";
    private static final String SCHEDULE_MS_TIMEOUT = "msTimeout";
    private static final String SCHEDULE_TICK_INTERVAL = "tickInterval";

    private final ScriptingClassResolver classResolver;
    private final ScriptManager scriptManager;
    private final ScriptScheduler scheduler;
    private final Map<String, Object> memberExports = new HashMap<>();

    public MqsUtilsAPI(ScriptingClassResolver classResolver, ScriptManager scriptManager, ScriptScheduler scheduler) {
        this.classResolver = classResolver;
        this.scriptManager = scriptManager;
        this.scheduler = scheduler;

        memberExports.put("chat", ChatUtils.class);
        memberExports.put("camera", CameraUtils.class);
        memberExports.put("assets", AssetIdentifiers.class);

        ProxyObject mathProxy = createMathProxy();
        ProxyObject mcProxy = createMcProxy();
        ProxyObject scheduleProxy = createScheduleProxy();

        memberExports.put(MATH, mathProxy);
        memberExports.put(MC, mcProxy);
        memberExports.put(SCHEDULE, scheduleProxy);
    }

    @Override
    public Object getMember(String key) {
        Object export = memberExports.get(key);
        if (export instanceof Class<?> utilClass) {
            return classResolver.getOrCreateWrapper(utilClass.getName());
        }
        return export;
    }

    @Override
    public Object getMemberKeys() {
        return memberExports.keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return memberExports.containsKey(key);
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
            private final Set<String> keys = Set.of(
                    MC_RAW,
                    MC_GET_MC,
                    MC_GET_PLAYER,
                    MC_GET_WORLD,
                    MC_CLIENT,
                    MC_PLAYER,
                    MC_WORLD,
                    MC_RUN_ON_CLIENT_THREAD
            );

            @Override
            public Object getMember(String key) {
                return switch (key) {
                    case MC_RAW -> classResolver.getOrCreateWrapper(McUtils.class.getName());
                    case MC_GET_MC -> (ProxyExecutable) _ -> McUtils.getMc();
                    case MC_GET_PLAYER -> (ProxyExecutable) _ -> McUtils.getPlayer();
                    case MC_GET_WORLD -> (ProxyExecutable) _ -> McUtils.getWorld();
                    case MC_CLIENT -> (ProxyExecutable) _ -> ScriptUtils.wrapReturn(
                            McUtils.getMc(),
                            classResolver.getMappingsManager(),
                            scriptManager
                    );
                    case MC_PLAYER -> (ProxyExecutable) _ -> McUtils.getPlayer()
                            .map(player -> ScriptUtils.wrapReturn(player, classResolver.getMappingsManager(), scriptManager))
                            .orElse(null);
                    case MC_WORLD -> (ProxyExecutable) _ -> McUtils.getWorld()
                            .map(world -> ScriptUtils.wrapReturn(world, classResolver.getMappingsManager(), scriptManager))
                            .orElse(null);
                    case MC_RUN_ON_CLIENT_THREAD -> (ProxyExecutable) args -> {
                        ApiArgumentChecks.requireArgCount(args, 1, "runOnClientThread requires a callback function.");
                        ApiArgumentChecks.requireExecutable(args, 0, "runOnClientThread requires a callback function.");
                        RunningScript owner = currentScript();
                        Value callback = args[0];
                        McUtils.getMc().execute(() -> executeCallback(owner, callback));
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
            private final Set<String> keys = Set.of(
                    SCHEDULE_TICK_TIMEOUT,
                    SCHEDULE_MS_TIMEOUT,
                    SCHEDULE_TICK_INTERVAL
            );

            @Override
            public Object getMember(String key) {
                return switch (key) {
                    case SCHEDULE_TICK_TIMEOUT -> (ProxyExecutable) args -> {
                        ensureCallback(args);
                        RunningScript owner = currentScript();
                        Value callback = args[0];
                        int delay = args.length > 1 ? Math.max(0, args[1].asInt()) : 0;
                        Runnable cancel = scheduler.scheduleTickTimeout(owner, callback, delay);
                        return toDisposer(owner, cancel);
                    };
                    case SCHEDULE_MS_TIMEOUT -> (ProxyExecutable) args -> {
                        ensureCallback(args);
                        RunningScript owner = currentScript();
                        Value callback = args[0];
                        long delay = args.length > 1 ? Math.max(0L, args[1].asLong()) : 0L;
                        Runnable cancel = scheduler.scheduleMsTimeout(owner, callback, delay);
                        return toDisposer(owner, cancel);
                    };
                    case SCHEDULE_TICK_INTERVAL -> (ProxyExecutable) args -> {
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
        ApiArgumentChecks.requireExecutable(args, 0, "First argument must be a callback function.");
        if (args.length > 1 && args[1] != null) {
            ApiArgumentChecks.requireNumber(args, 1, "Delay must be numeric.");
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
        ProxyExecutable exec = _ -> {
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
        } catch (IllegalStateException _) {
            // Ignore script cancellation exceptions
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
