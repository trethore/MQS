The `ConfigManager` provides a simple and persistent key-value storage for your script. Any data you save with it will be automatically written to a dedicated JSON file for your script in the `my-qol-scripts/configs/` folder.

This is the perfect way to store user settings, positions, lists, or any other data that needs to persist between game sessions.

### How It Works

*   **JSON-Based:** All data is stored in a standard JSON format. This means you can store numbers, strings, booleans, arrays, and nested objects.
*   **Automatic Loading:** Your script's configuration is loaded into memory the first time it is enabled or accessed.
*   **Automatic Saving:** The configuration is automatically saved when the game closes. You can also trigger a save manually.

### Core Methods

The `ConfigManager` provides a familiar interface for a key-value store.

*   **`ConfigManager.get(key, [defaultValue])`**: Retrieves a value from the config.
*   **`ConfigManager.set(key, value)`**: Sets or updates a value in the config.
*   **`ConfigManager.has(key)`**: Checks if a key exists in the config.
*   **`ConfigManager.save()`**: Manually saves the config to its file.
*   **`ConfigManager.getAll()`**: Returns the entire configuration as a JavaScript object.

#### `ConfigManager.get(key, [defaultValue])`
Retrieves a value associated with a given key.
*   **`key`** (string): The key of the value to retrieve.
*   **`defaultValue`** (any, optional): If the key does not exist in the config, this value will be returned instead. If not provided, `null` is returned.
*   **Returns**: The stored value, the default value, or `null`.

#### `ConfigManager.set(key, value)`
Creates a new entry or updates an existing one.
*   **`key`** (string): The key for the data you want to save.
*   **`value`** (any): The value to store. This can be any JSON-compatible type (string, number, boolean, array, or object).

#### `ConfigManager.has(key)`
Checks for the existence of a key.
*   **`key`** (string): The key to check.
*   **Returns**: `true` if the key exists, `false` otherwise.

#### `ConfigManager.save()`
Forces the current in-memory configuration to be written to its JSON file on the disk. This is useful if you want to ensure data is saved immediately without waiting for the game to close.

#### `ConfigManager.getAll()`
*   **Returns**: A JavaScript object representing the entire configuration for your script. You can manipulate this object directly, but changes will only be saved when `ConfigManager.save()` is called or the game closes.

---

### Example: Greeting with a Saved Name

This script greets the player using a name stored in the configuration. The first time it runs, it will use the default name "Toto". If you uncomment the `ConfigManager.set` line, it will save "Tata" as the name, and on subsequent enables, it will greet "Tata".

```javascript
// @module(main=TestConfigModule, name=Test Config Module, version=0.0.1)

const Text = net.minecraft.text.Text;

class TestConfigModule {
    onEnable() {
        println("Hello from Test Config Module!");

        // Get the 'name' from config. If it doesn't exist, default to "Toto".
        const name = ConfigManager.get("name", "Toto");

        // Uncomment the line below to change the saved name to "Tata".
        // The new name will be used the next time this script is enabled.
        // ConfigManager.set("name", "Tata");

        const player = MQS.utils.mc.player();
        if (player) {
            player.sendMessage(Text.literal(`Hello ${name}`), false);
        }
    }

    onDisable() {
        println("Goodbye from Test Config Module!");
        // The config is saved automatically when the game closes,
        // but you can call ConfigManager.save() here if needed.
    }
}

exportModule(TestConfigModule);
```

---

Next, we'll dive into the most advanced and powerful feature of MQS: the `HookManager`.

**➡️ Next Step: [[HookManager API|HookManager-API]]**
