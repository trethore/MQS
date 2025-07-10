package net.me.hooking;

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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unused")
public class HookInterceptor {

    public static final Map<String, CopyOnWriteArrayList<HookData>> HOOKS = new ConcurrentHashMap<>();
    public static final ThreadLocal<Deque<AdviceContext>> adviceContextStack = ThreadLocal.withInitial(ArrayDeque::new);
    public static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static void register(String hookId, Value jsCallback, RunningScript owner, ScriptManager scriptManager, Integer argCount) {
        HOOKS.computeIfAbsent(hookId, k -> new CopyOnWriteArrayList<>())
                .addFirst(new HookData(jsCallback, owner, scriptManager, argCount));
    }

    public static void unregister(String hookId, RunningScript owner, Integer argCount) {
        CopyOnWriteArrayList<HookData> hookList = HOOKS.get(hookId);
        if (hookList != null) {
            boolean removed = hookList.removeIf(data -> data.owner().equals(owner) && Objects.equals(data.argCount(), argCount));
            if (removed) {
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
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
            @Advice.This(optional = true) Object thiz
    ) {
        String hookId = method.getDeclaringClass().getName() + "::" + method.getName();
        CopyOnWriteArrayList<HookData> allHooksForName = HOOKS.get(hookId);

        if (allHooksForName == null || allHooksForName.isEmpty()) {
            return false;
        }

        CopyOnWriteArrayList<HookData> filteredHooks = new CopyOnWriteArrayList<>();
        for (HookData hookData : allHooksForName) {
            if (hookData.argCount() == null || hookData.argCount().equals(args.length)) {
                filteredHooks.add(hookData);
            }
        }

        if (filteredHooks.isEmpty()) {
            return false;
        }

        adviceContextStack.get().push(new AdviceContext());

        MappingsManager mappingsManager = Main.getInstance().getMappingsManager();
        HookContext hookContext = new HookContext(thiz, method, STACK_WALKER, mappingsManager);

        ProxyExecutable nextInChain = buildChain(hookContext, filteredHooks);

        try {
            Value[] initialChainArgs = new Value[args.length];
            for (int i = 0; i < args.length; i++) {
                initialChainArgs[i] = Value.asValue(args[i]);
            }
            Value result = (Value) nextInChain.execute(initialChainArgs);

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

    public static @NotNull ProxyExecutable buildChain(HookContext hookContext, CopyOnWriteArrayList<HookData> hookList) {

        ProxyExecutable nextInChain = passedArgs1 -> {
            Deque<AdviceContext> stack = adviceContextStack.get();
            if (stack != null && !stack.isEmpty()) {
                AdviceContext context = stack.peek();
                context.setShouldExecuteOriginal(true);
                context.setModifiedArgs(passedArgs1);
                context.setNextCalled(true);
            }
            return null;
        };

        for (int i = hookList.size() - 1; i >= 0; i--) {
            final HookData data = hookList.get(i);
            final ProxyExecutable finalNextInChain = nextInChain;
            nextInChain = passedArgs -> {
                RunningScript previousScript = data.scriptManager().getCurrentScript();
                data.scriptManager().setCurrentScript(data.owner());
                try {
                    Value jsArgsArray = data.owner().getContext().eval("js", "[]");
                    for (Value arg : passedArgs) {
                        Object javaObject = ScriptUtils.unwrapReceiver(arg);
                        Object customProxy = ScriptUtils.wrapReturn(javaObject);
                        jsArgsArray.invokeMember("push", customProxy);
                    }

                    return data.jsCallback().execute(
                            hookContext,
                            jsArgsArray,
                            data.owner().getContext().asValue(finalNextInChain)
                    );
                } finally {
                    data.scriptManager().setCurrentScript(previousScript);
                }
            };
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

    public record HookData(Value jsCallback, RunningScript owner, ScriptManager scriptManager, Integer argCount) {
    }

    public static class AdviceContext {
        private boolean shouldExecuteOriginal = false;
        private boolean isNextCalled = false; // Add this flag
        private Value scriptReturnValue = null;
        private Value[] modifiedArgs = null;

        public boolean isNextCalled() {
            return isNextCalled;
        }

        public void setNextCalled(boolean nextCalled) {
            isNextCalled = nextCalled;
        }

        public boolean shouldExecuteOriginal() {
            return shouldExecuteOriginal;
        }

        public void setShouldExecuteOriginal(boolean shouldExecuteOriginal) {
            this.shouldExecuteOriginal = shouldExecuteOriginal;
        }

        public boolean hasScriptReturnValue() {
            return scriptReturnValue != null && !scriptReturnValue.isNull();
        }

        public Value getScriptReturnValue() {
            return scriptReturnValue;
        }

        public void setScriptReturnValue(Value scriptReturnValue) {
            this.scriptReturnValue = scriptReturnValue;
        }

        public Value[] modifiedArgs() {
            return modifiedArgs;
        }

        public void setModifiedArgs(Value[] modifiedArgs) {
            this.modifiedArgs = modifiedArgs;
        }
    }
}