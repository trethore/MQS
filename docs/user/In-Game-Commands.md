While the GUI is great for general management, My QOL Scripts (MQS) also provides a comprehensive set of chat commands for quick and direct control. All MQS commands are **client-side**, meaning they won't be sent to the server and won't be seen by other players.

### Command Structure

All MQS commands start with `/mqs`. The general structure is:

`/mqs <sub-command> [arguments...]`

---

### Core Commands

#### `/mqs`
The base command with no arguments.
*   **Action:** Opens the main script management GUI.
*   **Alias:** `/mqs screen allscripts`

#### `/mqs update`
*   **Action:** Checks for new versions of the MQS mod itself on GitHub. If an update is found, it will display the changelog and begin downloading the new version. The update will be applied the next time you restart Minecraft.
*   **Note:** This command cannot be used in a development environment.

---

### Script Management (`/mqs script`)

These commands allow you to manage your scripts directly from chat. They are especially useful for keybinds or automated systems.

#### `/mqs script list`
*   **Action:** Prints a list of all available scripts and their current status (enabled/disabled) directly into your chat.

#### `/mqs script enable <script_id>`
*   **Action:** Enables a script.
*   **Argument:**
    *   `script_id`: The ID of the script you want to enable. The ID is the script's filename and its module name, joined by a colon (e.g., `my-script.js:My Cool Script`). You can use tab-completion to see a list of disabled scripts.
*   **Example:** `/mqs script enable "auto-sprint.js:Auto Sprint"`

#### `/mqs script disable <script_id>`
*   **Action:** Disables a currently running script.
*   **Argument:**
    *   `script_id`: The ID of the script to disable. Tab-completion will suggest currently running scripts.

#### `/mqs script reload <script_id>`
*   **Action:** A shortcut for disabling and then immediately re-enabling a script. This is useful for reloading a script after you've saved changes to its file.
*   **Argument:**
    *   `script_id`: The ID of the script to reload.

#### `/mqs script refresh`
*   **Action:** Rescans the scripts folder and disables all currently running scripts. This is useful if you have added or removed script files and want to start fresh.

#### `/mqs script refreshandreenable`
*   **Action:** Rescans the scripts folder and then attempts to re-enable all scripts that were running before the refresh. This is the same action performed by the "Refresh" button in the GUI.

#### `/mqs script save <script_id>`
*   **Action:** Manually saves the configuration for a specific running script.
*   **Argument:**
    *   `script_id`: The ID of the script whose config you want to save.

#### `/mqs script saveall`
*   **Action:** Saves the configurations for all currently running scripts. This is done automatically when you close the game, but can be triggered manually if needed.

---

### Screen Management (`/mqs screen`)

These commands provide direct access to the various MQS screens.

*   `/mqs screen allscripts` - Opens the main script list GUI.
*   `/mqs screen moreoptions` - Opens the "More Options" screen.
*   `/mqs screen keybinds` - Opens the keybind configuration screen.
*   `/mqs screen settings` - Opens the MQS settings screen.
*   `/mqs screen console` - Opens the MQS console interface.

---

For even more advanced control, check out the MQS Console, which has its own set of commands.

**➡️ Next Step: [[The MQS Console|The-MQS-Console]]**