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
import net.me.utils.ChatUtils;
import net.me.utils.McUtils;
import net.me.utils.ScriptScheduler;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.Set;

public class MqsUtilsAPI implements ProxyObject {
    private static final String MC = "mc";
    private static final String RUN_ON_CLIENT_THREAD = "runOnClientThread";
    private static final String WORLD = "world";
    private static final String PLAYER = "player";
    private static final String SCHEDULER = "scheduler";
    private static final String SCHEDULER_TIMEOUT = "timeout";
    private static final String SCHEDULER_INTERVAL = "interval";
    private static final String CHAT = "chat";
    private static final String MATH = "math";

    private final ScriptingClassResolver classResolver;
    private final ScriptManager scriptManager;
    private final ScriptScheduler scriptScheduler;
    private final ProxyObject schedulerProxy;
    private final Set<String> memberKeys;

    public MqsUtilsAPI(ScriptingClassResolver classResolver, ScriptManager scriptManager, ScriptScheduler scheduler) {
        this.classResolver = classResolver;
        this.scriptManager = scriptManager;
        this.scriptScheduler = scheduler;
        this.schedulerProxy = createSchedulerProxy();
        this.memberKeys = Set.of(
                MC,
                RUN_ON_CLIENT_THREAD,
                WORLD,
                PLAYER,
                SCHEDULER,
                CHAT,
                MATH
        );
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case MC -> mcMember();
            case RUN_ON_CLIENT_THREAD -> runOnClientThreadMember();
            case WORLD -> worldMember();
            case PLAYER -> playerMember();
            case SCHEDULER -> schedulerProxy;
            case CHAT -> chatMember();
            case MATH -> mathMember();
            default -> null;
        };
    }

    @Override
    public Object getMemberKeys() {
        return memberKeys.toArray(new String[0]);
    }

    @Override
    public boolean hasMember(String key) {
        return memberKeys.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Cannot modify the MQS.utils object.");
    }

    private ProxyObject createSchedulerProxy() {
        return new ProxyObject() {
            private final Set<String> keys = Set.of(
                    SCHEDULER_TIMEOUT,
                    SCHEDULER_INTERVAL
            );

            @Override
            public Object getMember(String key) {
                return switch (key) {
                    case SCHEDULER_TIMEOUT -> schedulerTimeoutMember();
                    case SCHEDULER_INTERVAL -> schedulerIntervalMember();
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
                throw new UnsupportedOperationException("Cannot modify MQS.utils.scheduler.");
            }
        };
    }

    private ProxyExecutable mcMember() {
        return ignored -> ScriptUtils.wrapReturn(
                McUtils.getMc(),
                classResolver.getMappingsManager(),
                scriptManager
        );
    }

    private ProxyExecutable runOnClientThreadMember() {
        return this::runOnClientThread;
    }

    private ProxyExecutable worldMember() {
        return ignored -> McUtils.getWorld()
                .map(world -> ScriptUtils.wrapReturn(world, classResolver.getMappingsManager(), scriptManager))
                .orElse(null);
    }

    private ProxyExecutable playerMember() {
        return ignored -> McUtils.getPlayer()
                .map(player -> ScriptUtils.wrapReturn(player, classResolver.getMappingsManager(), scriptManager))
                .orElse(null);
    }

    private Object chatMember() {
        return classResolver.getOrCreateWrapper(ChatUtils.class.getName());
    }

    private Object mathMember() {
        return classResolver.getOrCreateWrapper(Math.class.getName());
    }

    private ProxyExecutable schedulerTimeoutMember() {
        return args -> {
            ensureCallback(args);
            RunningScript owner = currentScript();
            Value callback = args[0];
            int delay = args.length > 1 ? Math.max(0, args[1].asInt()) : 0;
            Runnable cancel = scriptScheduler.scheduleTickTimeout(owner, callback, delay);
            return toDisposer(owner, cancel);
        };
    }

    private ProxyExecutable schedulerIntervalMember() {
        return args -> {
            ensureCallback(args);
            if (args.length < 2 || !args[1].isNumber()) {
                throw new IllegalArgumentException("interval requires an interval in ticks.");
            }
            RunningScript owner = currentScript();
            Value callback = args[0];
            int interval = Math.max(1, args[1].asInt());
            Runnable cancel = scriptScheduler.scheduleTickInterval(owner, callback, interval);
            return toDisposer(owner, cancel);
        };
    }

    private Object runOnClientThread(Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 1, "runOnClientThread requires a callback function.");
        ApiArgumentChecks.requireExecutable(args, 0, "runOnClientThread requires a callback function.");
        RunningScript owner = currentScript();
        Value callback = args[0];
        McUtils.getMc().execute(() -> executeCallback(owner, callback));
        return null;
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
        ProxyExecutable exec = ignored -> {
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
