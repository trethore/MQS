The `KeybindManager` API allows you to register custom keybinds that can trigger actions in your scripts. These keybinds are automatically saved per-script and can be configured by users in the "Keybinds" screen of the MQS menu.

### Core Methods

Like the `EventManager`, the `KeybindManager` API is straightforward and consists of two main functions:

*   **`KeybindManager.register(...)`**: Creates and registers a new keybind.
*   **`KeybindManager.unregister(...)`**: Removes a keybind.

#### `KeybindManager.register(name, defaultKey, callback, [isRepeatable], [debounceMs])`

This function registers a new keybind linked to your script.

*   **`name`** (string): The name of the keybind as it will appear in the GUI (e.g., "Toggle Sprint"). **This is required.**
*   **`defaultKey`** (integer): The default [GLFW Key Code](https://www.glfw.org/docs/latest/group__keys.html) for this keybind. Users can change this later. To leave a keybind unbound by default, use `-1`. **This is required.**
*   **`callback`** (function): The JavaScript function that will be executed when the key is pressed. **This is required.**
*   **`isRepeatable`** (boolean, optional, default: `false`): If `true`, the `callback` function will be fired continuously every tick while the key is held down. If `false`, it only fires once on the initial press.
*   **`debounceMs`** (integer, optional, default: `100`): The cooldown in milliseconds after a key is released before it can be pressed again. This prevents accidental double-presses.

#### `KeybindManager.unregister('name')`

This function removes a keybind that you previously registered. You **must** call this in your script's `onDisable()` method to ensure your keybinds are properly cleaned up.

*   **`name`** (string): The name of the keybind you want to unregister. This must match the name you used to register it.

You can also call `KeybindManager.unregisterAll()` with no arguments to unregister all keybinds owned by the current script.

---

### Example: A Repeatable Action Keybind

This script registers a keybind (`B`) that, when held down, will continuously send a message to the chat.

```javascript
// @module(main=TestKeybind, name=Test Keybind Module, version=0.0.1)

const Text = net.minecraft.text.Text;

// The GLFW key code for the 'B' key.
const KEY_CODE_B = 66;

class TestKeybind {
    onEnable() {
        println("Hello from Test Keybind Module!");

        // Register the keybind.
        KeybindManager.register(
            "test_keybind",     // Name for the GUI
            KEY_CODE_B,         // Default key is 'B'
            this.onKeyPress.bind(this),
            true                // isRepeatable is true, so it fires while held
        );
    }

    onDisable() {
        println("Goodbye from Test Keybind Module!");
        // It's crucial to unregister the keybind to clean up properly.
        KeybindManager.unregister("test_keybind");
    }

    onKeyPress() {
        const player = MQS.utils.mc.player();
        if (player) {
            player.sendMessage(Text.literal("Key 'B' is being held down!"), false);
        }
    }
}

exportModule(TestKeybind);
```

### How to Find Key Codes

The `defaultKey` parameter requires a numerical **GLFW key code**. You can find a complete list on the [official GLFW documentation website](https://www.glfw.org/docs/latest/group__keys.html).

Here are some common key codes:

| Key | Code | | Key | Code |
|---|---|---|---|---|
| A - Z | 65 - 90 | | `SPACE` | 32 |
| 0 - 9 | 48 - 57 | | `ENTER` | 257 |
| F1 - F12 | 290 - 301 | | `SHIFT` | 340 |
| `ESCAPE` | 256 | | `CONTROL` | 341 |
| Mouse Button 1 (Left) | 0 | | Mouse Button 2 (Right) | 1 |
| Mouse Button 3 (Middle) | 2 | | Mouse Button 4 (Side) | 3 |

---

Next, we'll cover how to save data and settings for your script.

**➡️ Next Step: [[ConfigManager API|ConfigManager-API]]**
