package net.me.hooking;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@SuppressWarnings("unused")
public class HookInterceptor {

    public static final Map<String, CopyOnWriteArrayList<HookData>> HOOKS = new ConcurrentHashMap<>();
    public static final ThreadLocal<AdviceContext> adviceContext = new ThreadLocal<>();
    public static final Map<String, ProxyExecutable> CHAIN_CACHE = new ConcurrentHashMap<>();

    public static void register(String hookId, Value jsCallback, RunningScript owner, ScriptManager scriptManager) {
        HOOKS.computeIfAbsent(hookId, k -> new CopyOnWriteArrayList<>())
                .addFirst(new HookData(jsCallback, owner, scriptManager));
        CHAIN_CACHE.remove(hookId);
        Main.LOGGER.info("Registered hook: {}", hookId);
    }

    public static void unregister(String hookId, RunningScript owner) {
        CopyOnWriteArrayList<HookData> hookList = HOOKS.get(hookId);
        if (hookList != null) {
            boolean removed = hookList.removeIf(data -> data.owner().equals(owner));
            if (removed) {
                CHAIN_CACHE.remove(hookId);
                Main.LOGGER.info("Unregistered hook owned by '{}': {}", owner.getName(), hookId);
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

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onEnter(
            @Advice.Origin Method method,
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args
    ) {
        String hookId = method.getDeclaringClass().getName() + "::" + method.getName();
        CopyOnWriteArrayList<HookData> hookList = HOOKS.get(hookId);

        if (hookList == null || hookList.isEmpty()) {
            return false;
        }

        // --- REFACTORED: Replaced lambda with an explicit inner class instance ---
        ProxyExecutable nextInChain = CHAIN_CACHE.computeIfAbsent(hookId, new ChainBuilder(method, hookList));

        try {
            Value[] initialChainArgs = new Value[args.length];
            for (int i = 0; i < args.length; i++) {
                initialChainArgs[i] = Value.asValue(args[i]);
            }
            Object raw = nextInChain.execute(initialChainArgs);
            Value result = (Value) raw;

            AdviceContext context = adviceContext.get();
            if (context != null && context.shouldExecuteOriginal()) {
                Value[] newArgs = context.modifiedArgs();
                if (newArgs != null && newArgs.length == args.length) {
                    for (int i = 0; i < args.length; i++) {
                        args[i] = ScriptUtils.unwrapArgs(
                                new Value[]{newArgs[i]},
                                new Class<?>[]{args[i].getClass()}
                        )[0];
                    }
                }
                adviceContext.set(new AdviceContext(true, null, null));
                return false;
            } else {
                boolean hasReturnValue = method.getReturnType() != void.class
                        && method.getReturnType() != Void.class;
                if (hasReturnValue) {
                    Object unwrapped = ScriptUtils.unwrapArgs(
                            new Value[]{result},
                            new Class<?>[]{method.getReturnType()}
                    )[0];
                    adviceContext.set(new AdviceContext(false, unwrapped, null));
                } else {
                    adviceContext.set(new AdviceContext(false, null, null));
                }
                return true;
            }
        } catch (Exception e) {
            Main.LOGGER.error("JS hook chain error in {}#{}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(), e);
            adviceContext.set(new AdviceContext(true, null, null));
            return false;
        }
    }

    public static @NotNull ProxyExecutable buildChain(Method method, CopyOnWriteArrayList<HookData> hookList) {
        ProxyExecutable nextInChain = passedArgs -> {
            adviceContext.set(new AdviceContext(true, null, passedArgs));
            return null;
        };

        for (int i = hookList.size() - 1; i >= 0; i--) {
            final HookData data = hookList.get(i);
            final ProxyExecutable finalNextInChain = nextInChain;
            nextInChain = passedArgs -> {
                data.scriptManager().setCurrentScript(data.owner());
                try {
                    return data.jsCallback().execute(
                            ScriptUtils.wrapReturn(method),
                            data.owner().getContext().asValue(ScriptUtils.unwrapArgs(passedArgs, null)),
                            data.owner().getContext().asValue(finalNextInChain)
                    );
                } finally {
                    data.scriptManager().clearCurrentScript();
                }
            };
        }
        return nextInChain;
    }

    @SuppressWarnings({"UnusedAssignment", "ParameterCanBeLocal"})
    @Advice.OnMethodExit
    public static void onExit(
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returnValue
    ) {
        AdviceContext context = adviceContext.get();
        if (context == null) {
            return;
        }
        if (!context.shouldExecuteOriginal()) {
            returnValue = context.overriddenReturnValue();
        }
        adviceContext.remove();
    }

    public static class ChainBuilder implements Function<String, ProxyExecutable> {
        private final Method method;
        private final CopyOnWriteArrayList<HookData> hookList;

        public ChainBuilder(Method method, CopyOnWriteArrayList<HookData> hookList) {
            this.method = method;
            this.hookList = hookList;
        }

        @Override
        public ProxyExecutable apply(String hookId) {
            // The logic from the lambda is now in this public method
            return buildChain(method, hookList);
        }
    }

    public record HookData(Value jsCallback, RunningScript owner, ScriptManager scriptManager) {
    }

    public record AdviceContext(boolean shouldExecuteOriginal, Object overriddenReturnValue, Value[] modifiedArgs) {
    }
}