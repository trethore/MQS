package net.me.hooking;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.me.Main;
import net.me.scripting.ScriptManager;
import net.me.scripting.module.RunningScript;
import net.me.scripting.utils.ScriptUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class HookInterceptor {

    public static final Map<String, HookData> HOOKS = new ConcurrentHashMap<>();
    public static final ThreadLocal<AdviceContext> adviceContext = new ThreadLocal<>();

    // --- Helper Methods ---
    public static void register(String hookId, Value jsCallback, RunningScript owner, ScriptManager scriptManager) {
        HOOKS.put(hookId, new HookData(jsCallback, owner, scriptManager));
        Main.LOGGER.info("Registered hook: {}", hookId);
    }

    public static void unregister(String hookId) {
        if (HOOKS.remove(hookId) != null) {
            Main.LOGGER.info("Unregistered hook: {}", hookId);
        }
    }

    public static boolean hasHook(String hookId) {
        return HOOKS.containsKey(hookId);
    }

    // --- Byte Buddy Advice ---
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onEnter(
            @Advice.Origin Method method,
            @Advice.AllArguments Object[] args
    ) {
        String hookId = method.getDeclaringClass().getName() + "::" + method.getName();
        HookData data = HOOKS.get(hookId);

        if (data == null) {
            return false;
        }

        AtomicReference<Boolean> shouldCallSuper = new AtomicReference<>(false);
        ProxyExecutable superProxy = new SuperCallProxy(shouldCallSuper);

        Main.LOGGER.debug("Hook (enter) found for {}, executing JS callback", hookId);
        data.scriptManager().setCurrentScript(data.owner());

        try {
            // FIX: Wrap each argument individually and pass as a JS array
            Object[] wrappedArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                wrappedArgs[i] = ScriptUtils.wrapReturn(args[i]);
            }

            Value result = data.jsCallback().execute(
                    ScriptUtils.wrapReturn(method),
                    data.owner().getContext().asValue(wrappedArgs),
                    data.owner().getContext().asValue(superProxy)
            );

            boolean hasReturnValue = method.getReturnType() != void.class && method.getReturnType() != Void.class;

            if (shouldCallSuper.get()) {
                adviceContext.set(new AdviceContext(true, null));
                return false;
            } else {
                if (hasReturnValue) {
                    Object unwrappedResult = ScriptUtils.unwrapArgs(new Value[]{result}, new Class<?>[]{method.getReturnType()})[0];
                    adviceContext.set(new AdviceContext(false, unwrappedResult));
                } else {
                    adviceContext.set(new AdviceContext(false, null));
                }
                return true;
            }
        } catch (Exception e) {
            Main.LOGGER.error("JS hook error {}#{} in {}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(), data.owner().getName(), e);
            adviceContext.set(new AdviceContext(true, null));
            return false;
        } finally {
            data.scriptManager().clearCurrentScript();
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
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

    // --- Public Inner Class to Replace Lambda ---
    public static class SuperCallProxy implements ProxyExecutable {
        private final AtomicReference<Boolean> shouldCallSuper;

        public SuperCallProxy(AtomicReference<Boolean> shouldCallSuper) {
            this.shouldCallSuper = shouldCallSuper;
        }

        @Override
        public Object execute(Value... arguments) {
            shouldCallSuper.set(true);
            return null;
        }
    }


    // --- Data Records ---
    public record HookData(Value jsCallback, RunningScript owner, ScriptManager scriptManager) {}
    public record AdviceContext(boolean shouldExecuteOriginal, Object overriddenReturnValue) {}
}