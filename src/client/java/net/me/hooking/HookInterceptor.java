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

package net.me.hooking;

import lombok.Getter;
import lombok.Setter;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.me.Main;
import net.me.hooking.context.HookContext;
import net.me.scripting.ScriptManager;
import net.me.scripting.mappings.MappingsManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@SuppressWarnings("unused")
public class HookInterceptor {

    public static final Map<String, CopyOnWriteArrayList<HookData>> HOOKS = new ConcurrentHashMap<>();
    public static final Map<CacheKey, ProxyExecutable> CHAIN_CACHE = new ConcurrentHashMap<>();
    public static final ThreadLocal<Deque<AdviceContext>> adviceContextStack = ThreadLocal.withInitial(ArrayDeque::new);
    public static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static ProxyExecutable createEmptyChainProxy() {
        return passedArgs -> {
            Deque<AdviceContext> stack = adviceContextStack.get();
            if (stack != null && !stack.isEmpty()) {
                AdviceContext context = stack.peek();
                if (context != null) {
                    context.setShouldExecuteOriginal(true);
                    context.setModifiedArgs(passedArgs);
                }
            }
            return null;
        };
    }

    public static void register(String hookId, Value jsCallback, RunningScript owner, ScriptManager scriptManager, Integer argCount) {
        HOOKS.computeIfAbsent(hookId, k -> new CopyOnWriteArrayList<>())
                .addFirst(new HookData(jsCallback, owner, scriptManager, argCount));

        CHAIN_CACHE.clear();
    }

    public static void unregister(String hookId, RunningScript owner, Integer argCount) {
        CopyOnWriteArrayList<HookData> hookList = HOOKS.get(hookId);
        if (hookList != null) {
            boolean removed = hookList.removeIf(data -> data.owner().equals(owner) && Objects.equals(data.argCount(), argCount));
            if (removed) {
                Main.LOGGER.info("Unregistered hook owned by '{}': {}", owner.getName(), hookId);
                CHAIN_CACHE.clear();
            }
            if (hookList.isEmpty()) {
                HOOKS.remove(hookId);
            }
        }
    }

    public static boolean hasHook(String hookId) {
        CopyOnWriteArrayList<HookData> hookList = HOOKS.get(hookId);
        return hookList != null && !hookList.isEmpty();
    }

    @Advice.OnMethodEnter
    public static boolean onEnter(
            @Advice.Origin Method method,
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
            @Advice.This(optional = true) Object thiz
    ) {
        String hookId = method.getDeclaringClass().getName() + "::" + method.getName();
        CopyOnWriteArrayList<HookData> allHooksForName = HOOKS.get(hookId);

        if (allHooksForName == null || allHooksForName.isEmpty()) {
            return false;
        }

        CacheKey cacheKey = new CacheKey(hookId, args.length);

        ProxyExecutable chain = CHAIN_CACHE.get(cacheKey);
        if (chain == null) {
            MappingsManager mappingsManager = Main.getInstance().getMappingsManager();
            ScriptManager scriptManager = allHooksForName.getFirst().scriptManager();
            ChainFactory factory = new ChainFactory(allHooksForName, thiz, method, scriptManager, mappingsManager);
            chain = factory.apply(cacheKey);
            CHAIN_CACHE.put(cacheKey, chain);
        }

        adviceContextStack.get().push(new AdviceContext());

        try {
            Value[] initialChainArgs = new Value[args.length];
            for (int i = 0; i < args.length; i++) {
                initialChainArgs[i] = Value.asValue(args[i]);
            }

            Value result = (Value) chain.execute(initialChainArgs);

            AdviceContext context = adviceContextStack.get().peek();
            if (context == null) {
                return false;
            }
            context.setScriptReturnValue(result);

            if (context.shouldExecuteOriginal()) {
                Value[] newArgs = context.modifiedArgs();
                if (newArgs != null && newArgs.length == args.length) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    for (int i = 0; i < args.length; i++) {
                        args[i] = ScriptUtils.unwrapArgs(
                                new Value[]{newArgs[i]},
                                new Class<?>[]{paramTypes[i]}
                        )[0];
                    }
                }
            }

            return !context.shouldExecuteOriginal();

        } catch (Exception e) {
            Main.LOGGER.error("JS hook chain error in {}#{}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(), e);
            AdviceContext context = adviceContextStack.get().peek();
            if (context != null) {
                context.setShouldExecuteOriginal(true);
            }
            return false;
        }
    }

    public static @NotNull ProxyExecutable rebuildChain(HookContext hookContext, List<HookData> filteredHooks, MappingsManager mappingsManager, ScriptManager scriptManager) {
        ProxyExecutable nextInChain = createEmptyChainProxy();

        for (int i = filteredHooks.size() - 1; i >= 0; i--) {
            final HookData data = filteredHooks.get(i);
            nextInChain = new HookExecutor(data, nextInChain, hookContext, mappingsManager, scriptManager);
        }
        return nextInChain;
    }

    @SuppressWarnings({"UnusedAssignment", "ParameterCanBeLocal"})
    @Advice.OnMethodExit
    public static void onExit(
            @Advice.Origin Method method,
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returnValue
    ) {
        Deque<AdviceContext> stack = adviceContextStack.get();
        if (stack.isEmpty()) {
            return;
        }

        AdviceContext context = stack.pop();

        if (context.hasScriptReturnValue()) {
            returnValue = ScriptUtils.unwrapArgs(
                    new Value[]{context.getScriptReturnValue()},
                    new Class<?>[]{method.getReturnType()}
            )[0];
        }
    }

    public static class ChainFactory implements Function<CacheKey, ProxyExecutable> {
        private final CopyOnWriteArrayList<HookData> allHooksForName;
        private final Object thiz;
        private final Method method;
        private final ScriptManager scriptManager;
        private final MappingsManager mappingsManager;

        public ChainFactory(CopyOnWriteArrayList<HookData> allHooksForName, Object thiz, Method method, ScriptManager scriptManager, MappingsManager mappingsManager) {
            this.allHooksForName = allHooksForName;
            this.thiz = thiz;
            this.method = method;
            this.scriptManager = scriptManager;
            this.mappingsManager = mappingsManager;
        }

        @Override
        public ProxyExecutable apply(CacheKey key) {
            List<HookData> filteredHooks = allHooksForName.stream()
                    .filter(hookData -> hookData.argCount() == null || hookData.argCount() == key.argCount())
                    .toList();

            if (filteredHooks.isEmpty()) {
                return createEmptyChainProxy();
            }

            HookContext hookContext = new HookContext(thiz, method, STACK_WALKER, mappingsManager, scriptManager);
            return rebuildChain(hookContext, filteredHooks, mappingsManager, scriptManager);
        }
    }

    public static class HookExecutor implements ProxyExecutable {
        private final HookData data;
        private final ProxyExecutable nextInChain;
        private final HookContext hookContext;
        private final MappingsManager mappingsManager;
        private final ScriptManager scriptManager;

        public HookExecutor(HookData data, ProxyExecutable nextInChain, HookContext hookContext, MappingsManager mappingsManager, ScriptManager scriptManager) {
            this.data = data;
            this.nextInChain = nextInChain;
            this.hookContext = hookContext;
            this.mappingsManager = mappingsManager;
            this.scriptManager = scriptManager;
        }

        @Override
        public Object execute(Value... passedArgs) {
            RunningScript previousScript = data.scriptManager().getCurrentScript();
            data.scriptManager().setCurrentScript(data.owner());
            try {
                Value jsArgsArray = data.owner().getContext().eval("js", "[]");
                for (Value arg : passedArgs) {
                    Object javaObject = ScriptUtils.unwrapReceiver(arg);
                    Object customProxy = ScriptUtils.wrapReturn(javaObject, mappingsManager, scriptManager);
                    jsArgsArray.invokeMember("push", customProxy);
                }

                return data.jsCallback().execute(
                        hookContext,
                        jsArgsArray,
                        data.owner().getContext().asValue(nextInChain)
                );
            } finally {
                data.scriptManager().setCurrentScript(previousScript);
            }
        }
    }

    public record CacheKey(String hookId, int argCount) {
    }

    public record HookData(Value jsCallback, RunningScript owner, ScriptManager scriptManager, Integer argCount) {
    }

    @Setter
    public static class AdviceContext {
        private boolean shouldExecuteOriginal = false;
        @Getter
        private boolean isNextCalled = false;
        @Getter
        private Value scriptReturnValue = null;
        private Value[] modifiedArgs = null;

        public boolean shouldExecuteOriginal() {
            return shouldExecuteOriginal;
        }

        public boolean hasScriptReturnValue() {
            return scriptReturnValue != null && !scriptReturnValue.isNull();
        }

        public Value[] modifiedArgs() {
            return modifiedArgs;
        }

    }
}