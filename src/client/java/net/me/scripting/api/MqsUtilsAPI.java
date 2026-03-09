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
import net.me.config.ConfigKeys;
import net.me.config.GlobalConfigManager;
import net.me.scripting.ScriptManager;
import net.me.scripting.api.internal.ScriptContextHelper;
import net.me.scripting.engine.ScriptingClassResolver;
import net.me.scripting.module.RunningScript;
import net.me.scripting.typings.MqsApiFragment;
import net.me.scripting.typings.TypingsConstants;
import net.me.scripting.typings.schema.TsObject;
import net.me.scripting.utils.ScriptUtils;
import net.me.utils.ChatUtils;
import net.me.utils.McUtils;
import net.me.utils.ScriptScheduler;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.List;
import java.util.Set;

import static net.me.scripting.typings.schema.TsDescriptors.*;

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
    private static final String OPTIONS = "options";
    private static final String MESSAGE = "message";
    private static final String PREFIX = "prefix";
    private static final String COMMAND = "command";
    private static final String LEVEL = "Level";
    private static final String CHAT_LEVEL_API = "MQSUtilsChatLevelApi";
    private static final String CHAT_LEVEL_INSTANCE = "MQSUtilsChatLevelInstance";
    private static final String MINECRAFT_CLASS = "net.minecraft.client.Minecraft";
    private static final String CLIENT_LEVEL_CLASS = "net.minecraft.client.multiplayer.ClientLevel";
    private static final String LOCAL_PLAYER_CLASS = "net.minecraft.client.player.LocalPlayer";
    private static final String MAPPED_INSTANCE_PREFIX = "MQSMappedInstance<\"";
    private static final String MAPPED_INSTANCE_SUFFIX = "\">";

    private final ScriptingClassResolver classResolver;
    private final ScriptManager scriptManager;
    private final ScriptScheduler scriptScheduler;
    private final GlobalConfigManager globalConfigManager;
    private final ScriptContextHelper contextHelper;
    private final ProxyObject schedulerProxy;
    private final ProxyObject optionsProxy;
    private final Set<String> memberKeys;

    public MqsUtilsAPI(ScriptingClassResolver classResolver, ScriptManager scriptManager, ScriptScheduler scheduler, GlobalConfigManager globalConfigManager) {
        this.classResolver = classResolver;
        this.scriptManager = scriptManager;
        this.scriptScheduler = scheduler;
        this.globalConfigManager = globalConfigManager;
        this.contextHelper = new ScriptContextHelper(scriptManager);
        this.schedulerProxy = createSchedulerProxy();
        this.optionsProxy = createOptionsProxy();
        this.memberKeys = Set.of(
                MC,
                RUN_ON_CLIENT_THREAD,
                WORLD,
                PLAYER,
                SCHEDULER,
                CHAT,
                MATH,
                OPTIONS
        );
    }

    public static MqsApiFragment describeTypeScript() {
        return new MqsApiFragment(
                List.of(
                        alias(CHAT_LEVEL_INSTANCE, "JavaInstance"),
                        alias(
                                CHAT_LEVEL_API,
                                "JavaClass<" + CHAT_LEVEL_INSTANCE + "> & { readonly ERROR: " + CHAT_LEVEL_INSTANCE + "; readonly INFO: " + CHAT_LEVEL_INSTANCE + "; readonly WARN: " + CHAT_LEVEL_INSTANCE + "; readonly SUCCESS: " + CHAT_LEVEL_INSTANCE + "; }"
                        )
                ),
                List.of(),
                List.of(),
                List.of(describeSchedulerApi(), describeChatApi(), describeOptionsApi(), describeUtilsApi())
        );
    }

    private static TsObject describeSchedulerApi() {
        return new TsObject(
                "MQSUtilsSchedulerApi",
                List.of(
                        method(SCHEDULER_TIMEOUT, fn(TypingsConstants.MQS_DISPOSER, p(TypingsConstants.CALLBACK, TypingsConstants.MQS_ANY_FUNCTION), opt("delayTicks", TypingsConstants.NUMBER))),
                        method(SCHEDULER_INTERVAL, fn(TypingsConstants.MQS_DISPOSER, p(TypingsConstants.CALLBACK, TypingsConstants.MQS_ANY_FUNCTION), p("intervalTicks", TypingsConstants.NUMBER)))
                )
        );
    }

    private static TsObject describeUtilsApi() {
        return new TsObject(
                "MQSUtilsApi",
                List.of(
                        method(MC, fn(mappedInstanceType(MINECRAFT_CLASS))),
                        method(RUN_ON_CLIENT_THREAD, fn(TypingsConstants.VOID, p(TypingsConstants.CALLBACK, TypingsConstants.MQS_ANY_FUNCTION))),
                        method(WORLD, fn(mappedInstanceType(CLIENT_LEVEL_CLASS) + " | null")),
                        method(PLAYER, fn(mappedInstanceType(LOCAL_PLAYER_CLASS) + " | null")),
                        ro(SCHEDULER, "MQSUtilsSchedulerApi"),
                        ro(CHAT, "MQSUtilsChatApi"),
                        ro(MATH, "JavaClass<any>"),
                        ro(OPTIONS, "MQSUtilsOptionsApi")
                )
        );
    }

    private static TsObject describeOptionsApi() {
        return new TsObject(
                "MQSUtilsOptionsApi",
                List.of(
                        ro(ConfigKeys.LOG_REDIRECT, TypingsConstants.BOOLEAN),
                        ro(ConfigKeys.ALLOW_ALL_CLASSES, TypingsConstants.BOOLEAN),
                        ro(ConfigKeys.ADDITIONAL_SCRIPT_DIRS, "readonly string[]"),
                        ro(ConfigKeys.DEFAULT_IDE_COMMAND, TypingsConstants.STRING),
                        ro(ConfigKeys.DEFAULT_PROJECT_PATH, TypingsConstants.STRING)
                )
        );
    }

    private static TsObject describeChatApi() {
        return new TsObject(
                "MQSUtilsChatApi",
                List.of(
                        ro("TAG", TypingsConstants.STRING),
                        ro(LEVEL, CHAT_LEVEL_API),
                        method("sendChatMessage", fn(TypingsConstants.VOID, p(MESSAGE, TypingsConstants.STRING))),
                        method("sendChatCommand", fn(TypingsConstants.VOID, p(COMMAND, TypingsConstants.STRING))),
                        method("addInfoChatMessage", fn(TypingsConstants.VOID, p(MESSAGE, TypingsConstants.STRING), p(PREFIX, TypingsConstants.BOOLEAN))),
                        method("addWarnChatMessage", fn(TypingsConstants.VOID, p(MESSAGE, TypingsConstants.STRING), p(PREFIX, TypingsConstants.BOOLEAN))),
                        method("addErrorChatMessage", fn(TypingsConstants.VOID, p(MESSAGE, TypingsConstants.STRING), p(PREFIX, TypingsConstants.BOOLEAN))),
                        method("addSuccessChatMessage", fn(TypingsConstants.VOID, p(MESSAGE, TypingsConstants.STRING), p(PREFIX, TypingsConstants.BOOLEAN))),
                        method("addRawMessage", fn(TypingsConstants.VOID, p(MESSAGE, TypingsConstants.STRING))),
                        method("addChatMessage", fn(TypingsConstants.VOID, p(MESSAGE, TypingsConstants.STRING), p("level", CHAT_LEVEL_INSTANCE), p(PREFIX, TypingsConstants.BOOLEAN)))
                )
        );
    }

    private static String mappedInstanceType(String className) {
        return MAPPED_INSTANCE_PREFIX + className + MAPPED_INSTANCE_SUFFIX;
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
            case OPTIONS -> optionsProxy;
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

    private ProxyObject createOptionsProxy() {
        return new ProxyObject() {
            private final Set<String> keys = Set.of(
                    ConfigKeys.LOG_REDIRECT,
                    ConfigKeys.ALLOW_ALL_CLASSES,
                    ConfigKeys.ADDITIONAL_SCRIPT_DIRS,
                    ConfigKeys.DEFAULT_IDE_COMMAND,
                    ConfigKeys.DEFAULT_PROJECT_PATH
            );

            @Override
            public Object getMember(String key) {
                GlobalConfigManager.OptionsSnapshot optionsSnapshot = globalConfigManager.getOptionsSnapshot();
                return switch (key) {
                    case ConfigKeys.LOG_REDIRECT -> optionsSnapshot.logRedirect();
                    case ConfigKeys.ALLOW_ALL_CLASSES -> optionsSnapshot.allowAllClasses();
                    case ConfigKeys.ADDITIONAL_SCRIPT_DIRS ->
                            createReadOnlyStringArray(optionsSnapshot.additionalScriptDirs());
                    case ConfigKeys.DEFAULT_IDE_COMMAND -> optionsSnapshot.defaultIdeCommand();
                    case ConfigKeys.DEFAULT_PROJECT_PATH -> optionsSnapshot.defaultProjectPath();
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
                throw new UnsupportedOperationException("Cannot modify MQS.utils.options.");
            }
        };
    }

    private ProxyArray createReadOnlyStringArray(List<String> values) {
        return new ProxyArray() {
            @Override
            public Object get(long index) {
                if (index < 0 || index >= values.size()) {
                    return null;
                }
                return values.get((int) index);
            }

            @Override
            public void set(long index, Value value) {
                throw new UnsupportedOperationException("Cannot modify MQS.utils.options.additionalScriptDirs.");
            }

            @Override
            public long getSize() {
                return values.size();
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
            RunningScript owner = requireCurrentScript();
            Value callback = args[0];
            int delay = args.length > 1 ? Math.max(0, args[1].asInt()) : 0;
            Runnable cancel = scriptScheduler.scheduleTickTimeout(owner, callback, delay);
            return contextHelper.createIdempotentDisposer(owner, cancel);
        };
    }

    private ProxyExecutable schedulerIntervalMember() {
        return args -> {
            ensureCallback(args);
            if (args.length < 2 || !args[1].isNumber()) {
                throw new IllegalArgumentException("interval requires an interval in ticks.");
            }
            RunningScript owner = requireCurrentScript();
            Value callback = args[0];
            int interval = Math.max(1, args[1].asInt());
            Runnable cancel = scriptScheduler.scheduleTickInterval(owner, callback, interval);
            return contextHelper.createIdempotentDisposer(owner, cancel);
        };
    }

    private Object runOnClientThread(Value[] args) {
        ApiArgumentChecks.requireArgCount(args, 1, "runOnClientThread requires a callback function.");
        ApiArgumentChecks.requireExecutable(args, 0, "runOnClientThread requires a callback function.");
        RunningScript owner = requireCurrentScript();
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

    private RunningScript requireCurrentScript() {
        return contextHelper.require("MQS.utils");
    }

    private void executeCallback(RunningScript owner, Value callback) {
        contextHelper.executeWithScript(owner, () -> {
            try {
                callback.execute();
            } catch (IllegalStateException ignored) {
                // Ignore script cancellation exceptions
            } catch (Exception e) {
                Main.LOGGER.error("runOnClientThread callback threw for script '{}'", owner.getName(), e);
            }
        });
    }
}
