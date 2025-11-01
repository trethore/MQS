The `HookManager` is the most advanced and powerful API in MQS. It allows you to **intercept and modify the game's own Java method calls** directly from JavaScript. This can be used to change game logic, cancel events before they happen, or react to actions that don't have a corresponding Fabric event.

> **⚠️ WARNING: USE WITH EXTREME CAUTION!**
> The `HookManager` is a low-level tool. Misusing it can **easily crash your game** or cause unexpected behavior.
> *   You are directly modifying the game's code at runtime.
> *   Always unhook everything you hook in your `onDisable()` method.
> *   Hooks persist until unhooked. If your script crashes before `onDisable`, the hook may remain, causing issues until you restart the game.
> *   This API is for advanced users who understand the risks.

### The Concept: Method Hooking

Hooking lets you insert your own JavaScript function into the call stack of a Java method. When the game tries to call that Java method, your JavaScript function runs *first*. From there, you have complete control: you can inspect the arguments, change them, prevent the original method from running entirely, or even modify its return value.

### Core Methods

The API is simple, but its usage is complex.

*   **`HookManager.hook(TargetClass, 'methodName', [argCount], callback)`**: Injects your function into a Java method.
*   **`HookManager.unhook(TargetClass, 'methodName')`**: Removes your hook from a Java method.

#### `HookManager.hook(...)`

*   **`TargetClass`**: The Java class containing the method you want to hook (e.g., `net.minecraft.client.MinecraftClient`).
*   **`'methodName'`** (string): The **Yarn-mapped** name of the method to hook (e.g., `'setScreen'`). MQS handles remapping automatically.
*   **`argCount`** (integer, optional): The number of arguments for a specific method overload. Use this to target a specific version of a method if multiple exist with the same name.
*   **`callback`** (function): The JavaScript function that will be executed when the method is called.

#### The Hook Callback: `(method, args, chain)`

Your callback function is the heart of the hook. It receives three arguments that give you full control over the method call.

```javascript
function myHookCallback(method, args, chain) {
    // Your logic here...
}
```

*   **`method`**: A Java `Method` object representing the original method being hooked. You will rarely need to interact with this directly.
*   **`args`**: A JavaScript `Array` containing the arguments that were passed to the Java method.
*   **`chain`**: A special function that you **must call** to proceed to the next hook or the original Java method.

#### Controlling the Execution Flow

What you do inside your callback determines the outcome of the method call.

1.  **Observing and Continuing:** To simply observe a method call without changing anything, inspect the `args` and then call `chain` with the original arguments.
    ```javascript
    return chain(...args); // The '...' spread operator is important!
    ```

2.  **Modifying Arguments:** To change the arguments before the original method runs, modify the `args` array and then pass the modified array to `chain`.
    ```javascript
    args[0] = 'a-new-value';
    return chain(...args);
    ```

3.  **Canceling the Original Method:** To completely prevent the original Java method from being called, **do not call `chain`**. You can simply `return` from your function.
    ```javascript
    return; // We don't call chain, so the original method never runs.
    ```

4.  **Overriding the Return Value:** If you cancel the original method, you can also provide a new return value. Whatever your hook function returns will become the return value of the original call.
    ```javascript
    // Assume we're hooking a method that returns a boolean.
    // Cancel the original call and force it to return 'true'.
    return true;
    ```

### Unhooking

It is **absolutely critical** to unhook your methods in `onDisable()` to clean up properly.

*   `HookManager.unhook(TargetClass, 'methodName')`: Removes your script's hook from the specified method.
*   `HookManager.unhookAll()`: A convenient function that removes all hooks owned by the current script. **This is the recommended cleanup method.**

---

### Full Example: Screen Change Notifier

This script hooks the `setScreen` method on `MinecraftClient` to print a message to the console every time a new screen is opened or closed.

```javascript
// @module(main=TestHookModule, name=Test Hook Module, version=0.0.1)

const MinecraftClient = net.minecraft.client.MinecraftClient;

class TestHookModule {
    onEnable() {
        println("Hello from Test Hook Module!");

        // Hook the 'setScreen' method on the MinecraftClient class.
        HookManager.hook(MinecraftClient, "setScreen", this.myHookCallback);
    }

    onDisable() {
        println("Goodbye from Test Hook Module!");
        // Always clean up your hooks!
        HookManager.unhookAll();
    }

    myHookCallback(method, args, chain) {
        // The first argument to setScreen is the screen object, or null if closing.
        const screen = args[0];

        if (!screen) {
            println("Screen closed (setScreen was called with null).");
        } else {
            // We can even call methods on the screen object.
            println("Screen opened: " + screen.getTitle().getString());
        }

        // IMPORTANT: We must call chain() to allow the original
        // setScreen method to run. If we don't, no screens will ever open!
        return chain(...args);
    }
}

exportModule(TestHookModule);

```

---

Next, we'll cover the `MQSUtils` library, which provides a set of helpful tools for common tasks.

**➡️ Next Step: [[MQSUtils API|MQSUtils-API]]**

### Under the Hood

For those curious about the underlying technology, the `HookManager` is powered by the **[ByteBuddy](https://bytebuddy.net/#/tutorial)** library, which allows for dynamic Java code generation and modification at runtime.
