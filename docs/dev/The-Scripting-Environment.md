Every script you write runs within a carefully configured environment provided by MQS. This environment gives you access to powerful APIs, utility functions, and direct interoperability with Minecraft's own code. Understanding this environment is key to unlocking the full potential of MQS.

### The Engine: GraalJS

MQS uses the **GraalJS** engine, which is a high-performance implementation of JavaScript. It supports the modern **ECMAScript 2024** standard, so you can use features like `class`, `const`, `let`, arrow functions, async/await, and more.

> **New to JavaScript or GraalJS?**
> *   For a great introduction to the language, check out the **[W3Schools JavaScript Tutorial](https://www.w3schools.com/js/)**.
> *   To learn about the specific features and capabilities of the engine, see the **[Official GraalJS Documentation](https://www.graalvm.org/latest/reference-manual/js/)**.

---

### Global Objects & Functions

When your script is executed, MQS injects several global objects and functions into its scope. You can use these anywhere in your script without needing to import them.

#### Core Functions

*   `println(...)` and `print(...)`
    *   **Description:** Simple functions for printing output. This is very useful for quick debugging.
    *   **Output:** By default, output goes to the main game console/log file. If you enable [[Log Redirection|Configuration]], the output will appear directly in the [[The MQS Console|The-MQS-Console]].
    *   **Example:** `println("My value is: " + myVariable);`

*   `exportModule(YourClass)`
    *   **Description:** This function is essential for making your main script class visible to MQS. You must call this with your main class as the argument for your script to be discoverable.
    *   **Reference:** [[Creating Your First Script|Creating-Your-First-Script]]

#### Java Interoperability Functions

These functions are your primary tools for working with Java classes and objects from within JavaScript.

*   `importClass('...')`
    *   **Description:** Loads a Java class so you can interact with it. It returns a wrapper that allows you to call static methods, access static fields, and create new instances (`new MyClass()`).
    *   **Argument:** You must provide the **Yarn-mapped** fully-qualified class name (e.g., `net.minecraft.item.Items`).
    *   **Reference:** [[Java Interoperability|Java-Interoperability]]

*   `extendMapped({...})`
    *   **Description:** A powerful function that allows you to create a new JavaScript class that extends a Java class. This is the primary way to override game methods.
    *   **Reference:** [[Java Interoperability|Java-Interoperability]]

*   `wrap(javaObject)`
    *   **Description:** Takes a raw Java object (e.g., one you receive from an event callback) and wraps it in a proxy. This proxy allows you to access its methods and fields using their developer-friendly Yarn names instead of intermediary names.
    *   **Reference:** [[Java Interoperability|Java-Interoperability]]

#### Built-in API Managers

MQS provides several manager objects that expose the core APIs of the mod. Each of these is a gateway to a different subsystem.

*   **`EventManager`**: For registering and unregistering callbacks for in-game events.
    *   **Reference:** [[EventManager API|EventManager-API]]
*   **`CommandManager`**: For creating new client-side chat commands.
    *   **Reference:** [[CommandManager API|CommandManager-API]]
*   **`KeybindManager`**: For registering custom keybinds.
    *   **Reference:** [[KeybindManager API|KeybindManager-API]]
*   **`ConfigManager`**: For saving and loading script-specific data.
    *   **Reference:** [[ConfigManager API|ConfigManager-API]]
*   **`HookManager`**: (Advanced) For intercepting and modifying Java method calls.
    *   **Reference:** [[HookManager API|HookManager-API]]
*   **`MQSUtils`**: A collection of utility objects for common tasks like rendering and chat.
    *   **Reference:** [[MQSUtils API|MQSUtils-API]]

#### Global Packages

For convenience, the top-level packages for Minecraft, Fabric, and Mojang are available globally. This means you can access classes directly without needing to use `importClass` every time, which is great for readability.

**Example:**
```javascript
// This is equivalent to:
// const MinecraftClient = importClass('net.minecraft.client.MinecraftClient');
// const mc = MinecraftClient.getInstance();

const mc = MQS.utils.mc.client();
```
The available top-level packages include `net`, `com`, `org`, and more.

---

### The Scripting Context

It's important to remember that most MQS APIs need to know which script is calling them. MQS handles this automatically by tracking the "current script context." This works seamlessly as long as you call the APIs from within your script's lifecycle methods (`onEnable`, `onDisable`) or from a callback you registered (like for an event or command).

If you try to use an API from an asynchronous context (like a `setTimeout` that fires later), the context may be lost, and the API call will fail.

---

This covers the environment your scripts operate in. Now, let's dive into the specifics of each API, starting with the `EventManager`.

**➡️ Next Step: [[EventManager API|EventManager-API]]**
