# Performance Tuning (JVM Arguments)

My QOL Scripts uses the GraalJS engine, which can run in two modes: a slower "interpreted" mode and a much faster "JIT-compiled" mode. By default, Minecraft's JVM does not enable the features required for JIT compilation, so you may see a warning in your logs about a "fallback runtime."

For the best performance, especially when running complex scripts, it is **highly recommended** that you add the following JVM arguments to your Minecraft launch configuration.

---

### Recommended Arguments

Add the following arguments to your JVM configuration for a significant performance boost:

```
--add-modules=jdk.zipfs --add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED
```
*Note: These are standard arguments for modern Java versions on Fabric and are generally safe and beneficial for other mods as well.*

And for MQS specifically:
```
-XX:+EnableJVMCI -XX:+EnableDynamicAgentLoading
```

*   **`-XX:+EnableJVMCI`**: Enables the **Java Virtual Machine Compiler Interface**. This is the key that allows GraalJS to use its high-performance, just-in-time (JIT) compiler, making scripts run much faster.
*   **`-XX:+EnableDynamicAgentLoading`**: Required for the advanced `HookManager` API to function correctly. While you may not write hooks yourself, scripts you download might use this feature.

### Optional Argument to Hide Warnings

If you are unable or do not wish to enable JVMCI, you can hide the performance warning message from your logs with this argument:

*   **`-Dpolyglot.engine.WarnInterpreterOnly=false`**: Disables the console warning about GraalJS running in the slower interpreted mode.

---

### How to Add JVM Arguments

These instructions are for the official Minecraft Launcher. The process is similar for other launchers like Prism or the Modrinth App.

1.  Open the **Minecraft Launcher**.
2.  Go to the **"Installations"** tab.
3.  Hover over your Fabric installation and click the **three dots (...)**, then select **"Edit"**.
4.  Click **"More Options"** to reveal the "JVM Arguments" text box.
5.  In the text box, add the recommended arguments. It's best to add them to the beginning of the line.

    **Example:**
    Before:
    `-Xmx2G ...`

    After:
    `-XX:+EnableJVMCI -XX:+EnableDynamicAgentLoading -Xmx2G ...`

6.  Click **"Save"** and launch the game. The performance warning should now be gone, and your scripts will run much faster!
