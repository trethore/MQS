My QOL Scripts (MQS) uses a simple and powerful file-based configuration system. All configuration files are stored in the `my-qol-scripts` folder within your main Minecraft directory (`.minecraft`).

There are two types of configuration files: the **global config** for the mod itself, and **per-script configs** for individual scripts.

### Global Configuration (`mqs_config.json`)

This file controls the core settings of the MQS mod. It is located at:
`<Your Minecraft Folder>/my-qol-scripts/mqs_config.json`

You can edit these settings in-game via the [[Settings Screen|Settings-Screen]], or by editing the file directly with a text editor (the game must be closed).

Here are the available options:

*   **`logRedirect`** (boolean, default: `false`)
    *   If `true`, all of Minecraft's internal logs will be redirected to the [[The MQS Console|The-MQS-Console]]. This is extremely useful for debugging scripts, as it will show errors and `println` statements that might otherwise only appear in log files.
    *   This is the same setting controlled by the `logredirect` console command.

*   **`allowAllClasses`** (boolean, default: `false`)
    *   **⚠️ Security Warning:** This is a dangerous setting. By default, MQS restricts scripts to a safe subset of Java classes. Setting this to `true` gives scripts the ability to access any Java class, which could potentially be used for malicious purposes.
    *   **Only enable this if you are a developer or if you completely trust every script you have installed.**
    *   This is the same setting controlled by the `allowallclasses` console command. A script refresh is required for it to take effect.

*   **`additionalScriptDirs`** (array of strings, default: `[]`)
    *   Adds extra folders that MQS will scan for `@module` scripts alongside the default `<Your Minecraft Folder>/my-qol-scripts/scripts`.
    *   Relative entries are resolved from the `my-qol-scripts` directory, and absolute paths are supported on every platform.
    *   Example:
        ```json
        "additionalScriptDirs": [
          "../shared-scripts",
          "D:/scripts/mqs"
        ]
        ```

*   **`showEnableDisableToast`** (boolean, default: `true`)
    *   If `true`, a small "toast" notification will appear in the corner of your screen whenever you enable or disable a script, providing instant feedback.

---

### Per-Script Configuration (`configs/*.json`)

MQS provides every script with its own dedicated configuration file. This allows script creators to save settings, and it's also where MQS stores the keybinds you configure in the GUI.

These files are located in:
`<Your Minecraft Folder>/my-qol-scripts/configs/`

#### How it Works

*   **Automatic Creation:** A config file is created for a script the first time it is enabled or the first time it saves a value.
*   **Automatic Saving:** Configs for all running scripts are saved automatically when you close the game. You can also save them manually with the `/mqs script save` or `saveconfig` commands.
*   **Automatic Deletion:** To keep your folder clean, if a script is disabled and has no custom data saved (e.g., only `enabled: false`), its config file will be deleted automatically.
*   **Naming:** The filename is based on the script's unique ID (e.g., `auto-sprint.js:Auto Sprint` becomes `auto-sprint.js-Auto-Sprint.json`).

#### For Users

As a user, you will rarely need to edit these files directly. The main reason to look here would be to see what data a script is saving or to manually change a keybind's keycode.

#### For Developers

As a script developer, this is your personal storage space. You can read and write any data you want using the [[ConfigManager API|ConfigManager]]. The `enabled` key and a `keybinds` object are managed by MQS, but you are free to add any other keys you need.

**Example `configs/my-cool-script.js-My-Cool-Script.json`:**
```json
{
  "enabled": true,
  "keybinds": {
    "toggle-feature": 71
  },
  "someCustomSetting": "hello world",
  "anotherSetting": 123
}
