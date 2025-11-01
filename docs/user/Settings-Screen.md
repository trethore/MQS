# Settings Screen

The Settings screen allows you to configure the global behavior of the MQS mod. You can access it from the "More Options" menu in the main `/mqs` GUI or by running the command `/mqs screen settings`.

[Image of the Settings Screen]

Each setting corresponds to an entry in the global `mqs_config.json` file. See the [[Configuration|Configuration]] page for more technical details.

---

### Available Settings

The screen provides a simple toggle interface for the following options:

*   **Log Redirection**
    *   **Description:** When enabled, all of Minecraft's internal logs (and output from scripts using `println`) are redirected to the [[The MQS Console|The-MQS-Console]].
    *   **Use Case:** This is incredibly useful for script developers who need to see debug messages and error logs without checking the game's log files.
    *   **Default:** Disabled.

*   **Allow All Classes**
    *   **Description:** Toggles whether scripts are allowed to access any Java class on your system.
    *   **⚠️ Security Warning:** This is a dangerous setting. By default, MQS restricts scripts to a safe set of classes to prevent malicious code from running. Only enable this if you are a developer or if you absolutely trust every script you have installed.
    *   **Default:** Disabled.

*   **Enable/Disable Toasts**
    *   **Description:** When enabled, a small notification ("toast") will appear in the corner of your screen whenever you enable or disable a script from the GUI.
    *   **Default:** Enabled.