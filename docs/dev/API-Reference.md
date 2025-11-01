# MQS API Reference

This section serves as a central hub for the My QOL Scripts API documentation. Each page details a specific manager or utility library, explaining its purpose, methods, and providing practical examples.

Whether you're looking to create a simple command or a complex game modification, this is your guide to the tools available.

---

### Core APIs

*   **[[The Scripting Environment|The-Scripting-Environment]]**
    *   An overview of the global objects, functions, and context in which your scripts run. Start here to understand the landscape.

*   **[[EventManager API|EventManager-API]]**
    *   Learn how to listen and react to in-game events, from player actions to rendering ticks.

*   **[[KeybindManager API|KeybindManager-API]]**
    *   Create custom keybinds to trigger actions in your scripts.

*   **[[CommandManager API|CommandManager-API]]**
    *   Build powerful, client-side chat commands with arguments and tab-completion.

*   **[[ConfigManager API|ConfigManager-API]]**
    *   Save and load persistent data for your scripts, such as user settings or saved locations.

*   **[[HookManager API|HookManager-API]]** `(Advanced)`
    *   Dive deep into the game's code by intercepting and modifying Java method calls directly. Use with caution!

### Utility & Interoperability

*   **[[Java Interoperability|Java-Interoperability]]**
    *   Understand how to use `importClass`, `extendMapped`, and `wrap` to work seamlessly with Minecraft's Java classes.

*   **[[MQSUtils API|MQSUtils-API]]**
    *   Explore the collection of helper libraries for common tasks like 2D/3D rendering, chat messages, and color manipulation.

---

### Putting It All Together

*   **[[Creating Your First Script|Creating-Your-First-Script]]**
    *   A step-by-step tutorial for building a basic MQS script from scratch.

*   **[[Examples & Recipes|Examples-and-Recipes]]**
    *   A collection of complete, working scripts that demonstrate how to combine different APIs to achieve practical goals.

### External Resources

To help you on your scripting journey, here are some useful external resources:

*   **[W3Schools JavaScript Tutorial](https://www.w3schools.com/js/)**: An excellent resource for learning the fundamentals of JavaScript.
*   **[GraalJS Reference Manual](https://www.graalvm.org/latest/reference-manual/js/)**: The official documentation for the JavaScript engine MQS uses. Useful for understanding advanced features and compatibility.
*   **[Fabric Yarn Mappings Browser (1.21.4)](https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/)**: An essential tool for finding the correct names of Minecraft classes, methods, and fields to use in your scripts.
*   **[ByteBuddy Documentation](https://bytebuddy.net/#/tutorial)**: For the curious, this is the library MQS uses under the hood to power the `HookManager`.