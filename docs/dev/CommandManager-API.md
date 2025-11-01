The `CommandManager` allows you to create your own client-side chat commands. These commands can have sub-commands, arguments, and custom tab-completion suggestions, making them feel just like native Minecraft commands.

All commands created with this API are **client-side only**. They are never sent to the server and cannot be seen by other players.

### The Builder Pattern

Creating a command in MQS uses a "builder" pattern. You start by creating a command node (a literal or an argument) and then chain methods like `.then()` and `.executes()` to define its structure and behavior.

The main functions you'll use are:
*   **`CommandManager.literal('name')`**: Creates a literal command node (e.g., `/greet` or `hello`).
*   **`CommandManager.argument('name', type)`**: Creates an argument node (e.g., `<name>`).
*   **`.then(subBuilder)`**: Attaches a sub-command or argument to the current node.
*   **`.executes(callback)`**: Defines the action to run when the command is executed at this node.
*   **`.suggests(callback)`**: (For arguments only) Provides tab-completion suggestions.
*   **`CommandManager.register(builder)`**: Registers the final, constructed command.

### Command Structure Methods

#### `CommandManager.literal('commandName')`
This is the starting point for any new command or a literal sub-command.
*   **`commandName`** (string): The name of your command or sub-command (e.g., 'greet').
*   **Returns:** A `CommandBuilder` instance.
> Note: `CommandManager.builder('name')` is an alias for this function.

#### `.then(otherBuilder)`
This method nests another command builder within the current one, creating a subcommand structure.
*   **`otherBuilder`** (CommandBuilder): Another `CommandBuilder` instance, created with `CommandManager.literal(...)` or `CommandManager.argument(...)`.
*   **Returns:** The original `CommandBuilder`, so you can continue chaining methods.

#### `.executes(callback(context))`
This defines what happens when a user presses Enter on the command.
*   **`callback`** (function): A function that receives a `context` object as its argument.
*   **`context`** (JSCommandContext): An object giving you access to the command source and any arguments.
> Note: `.exec()` is a convenient alias for `.executes()`.

### Arguments

To accept arguments, you use `CommandManager.argument()` and chain it with `.then()`.

#### `CommandManager.argument(name, type)`
Creates an argument node.
*   **`name`** (string): A name for the argument (e.g., 'player'). This is used to retrieve its value from the context.
*   **`type`** (string): The type of argument. This is specified using a string corresponding to an `ArgType`.

#### `CommandManager.ArgType`
This is a conceptual object that holds all valid argument types. You provide them as strings.

| Type String | Aliases | Description |
|---|---|---|
| `'word'` | | A single word without spaces. |
| `'string'` | | A single word, or a quoted string. |
| `'greedy'` | `'text'` | Consumes the rest of the command input as a single string. |
| `'integer'` | `'int'` | A 32-bit integer. |
| `'long'` | | A 64-bit integer. |
| `'float'` | | A single-precision floating-point number. |
| `'double'` | | A double-precision floating-point number. |
| `'boolean'` | `'bool'`| `true` or `false`. |

#### `.suggests(callback(context))`
Provides tab-completion suggestions for an argument node.
*   **`callback`** (function): A function that should return an `Array` of strings to be suggested.
> Note: `.suggest()` is a convenient alias for `.suggests()`.

### The `JSCommandContext` Object
The `context` object passed to your `executes` and `suggests` callbacks is your tool for interacting with the command's state.

*   `context.getSource()`: Returns the `FabricClientCommandSource`, which contains references to the client, player, etc.
*   `context.getArgumentAsString('name')`: Gets a string argument by name.
*   `context.getArgumentAsInt('name')`: Gets an integer argument.
*   (...and so on for `Bool`, `Double`, `Float`, `Long`).

---

### Full Example

Let's create a command `/greet` that can say hello to a player, with tab-completion for player names.

```javascript
// @module(main=TestCommand, name=Test Command Module, version=0.0.1)

class TestCommand {
    onEnable() {
        // A fake list of players for our tab-completion
        const fakePlayerList = ["AwesomeDude", "CoolPlayer", "EpicGamer"];

        const rootBuilder = CommandManager.literal('greet')
            // The action for just typing `/greet`
            .executes(ctx => {
                MQSUtils.Chat.addInfoChatMessage("Please provide a subcommand, like 'hello'.");
            });

        const helloCommand = CommandManager.literal('hello')
            .then(
                CommandManager.argument('name', 'word') // Using the 'word' ArgType
                    // Provide suggestions for the 'name' argument
                    .suggests(ctx => fakePlayerList)
                    // The action for `/greet hello <name>`
                    .executes(ctx => {
                        const name = ctx.getArgumentAsString('name');
                        const message = `Hello ${name}! Welcome!`;
                        MQSUtils.Chat.addSuccessChatMessage(message);
                    })
            );

        // Assemble the command by adding 'hello' as a subcommand of 'greet'
        rootBuilder.then(helloCommand);

        // Register the final command structure
        CommandManager.register(rootBuilder);
    }

    onDisable() {
        // It's crucial to unregister the command to clean up properly.
        CommandManager.unregister('greet');
    }
}

exportModule(TestCommand);
```

### Unregistering Commands

It is crucial to unregister your commands in `onDisable()` to prevent them from persisting after your script is disabled.

*   `CommandManager.unregister('commandName')`: Unregisters a root command and all its sub-commands.
*   `CommandManager.unregisterAll()`: Unregisters all commands owned by the current script. This is often the easiest and safest option for cleanup.

---

Next, let's learn how to create and manage custom keybinds.

**➡️ Next Step: [[KeybindManager API|KeybindManager-API]]**