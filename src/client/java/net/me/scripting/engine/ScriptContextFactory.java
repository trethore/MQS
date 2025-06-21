package net.me.scripting.engine;

import net.me.Main;
import net.me.scripting.wrappers.LazyPackageProxy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScriptContextFactory {

    private final ScriptingClassResolver classResolver;

    public ScriptContextFactory(ScriptingClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    public Context createContext(ThreadLocal<Map<String, Value>> perFileExports) {
        Main.LOGGER.info("Creating new script context (ECMAScript 2024)...");
        long startTime = System.currentTimeMillis();
        Context newContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(classResolver::isClassAllowed)
                .option("js.ecmascript-version", "2024")
                .option("js.esm-eval-returns-exports", "true")
                .build();

        configureContext(newContext, perFileExports);

        long endTime = System.currentTimeMillis();
        Main.LOGGER.info("New script context created in {}ms.", (endTime - startTime));
        return newContext;
    }

    private void configureContext(Context context, ThreadLocal<Map<String, Value>> perFileExports) {
        registerPackages(context);

        var bindings = context.getBindings("js");

        bindings.putMember("importClass", ScriptingApi.createImportClassProxy(classResolver, context));
        bindings.putMember("extendMapped", ScriptingApi.createExtendMappedProxy(classResolver, context));
        bindings.putMember("wrap", ScriptingApi.createWrapProxy(classResolver));
        bindings.putMember("exportModule", ScriptingApi.createExportModuleProxy(perFileExports));

        bindings.putMember("println", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.println(arg);
            return null;
        });
        bindings.putMember("print", (ProxyExecutable) args -> {
            for (Value arg : args) System.out.print(arg);
            return null;
        });
    }

    private void registerPackages(Context context) {
        Set<String> topLevelPackages = new HashSet<>();
        if (classResolver.getKnownPackagePrefixes() != null) {
            for (String prefix : classResolver.getKnownPackagePrefixes()) {
                if (classResolver.isClassInMc(prefix)) {
                    topLevelPackages.add(prefix.split("\\.")[0]);
                }
            }
        }

        var bindings = context.getBindings("js");
        for (String pkg : topLevelPackages) {
            if (!bindings.hasMember(pkg)) {
                bindings.putMember(pkg, new LazyPackageProxy(pkg, this.classResolver));
            }
        }
    }
}