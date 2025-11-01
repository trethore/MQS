The main MQS Graphical User Interface (GUI) is your central hub for managing all your scripts. It's designed to be simple and intuitive.

### How to Open the GUI

You can open the main GUI at any time by typing the following command in chat:

`/mqs`

You can also open it by pressing its dedicated keybind. See the [[Keybinds Screen|Keybinds-Screen]] page to learn how to configure it.

---

### GUI Overview

Here is a breakdown of the main screen, which is based on `AllScriptsScreen.java` from the source code.

[Image of the full AllScriptsScreen GUI]

#### 1. Title
At the top, you'll see the title "My QOL Scripts," rendered with a rainbow effect.

#### 2. Search Bar
Located below the title, the search bar allows you to quickly filter your scripts.

*   **Functionality:** Simply start typing the name of a script, and the list will automatically update to show only matching results.
*   **Clear Button:** The `X` button to the right of the search bar will instantly clear your search query.

[Image of the search bar and clear button]

#### 3. Script List
This is the core of the GUI, displaying all your discovered scripts.

*   **Pagination:** Only a few scripts are shown at a time. Use the "Previous" and "Next" buttons at the bottom to navigate through pages.
*   **Script Entry:** Each entry shows:
    *   **Module Name & Version:** The script's display name and version, as defined in its `@module` tag.
    *   **File Name:** The name of the `.js` file the script comes from.
    *   **Enable/Disable Toggle:** The switch on the right controls the script's state.
        *   **Green:** The script is currently active and running.
        *   **Red:** The script is disabled.
        *   Clicking the toggle will immediately enable or disable the script and show a small toast notification in the corner of your screen (if enabled in settings).

[Image of a script entry, showing the name, file, and toggle]

#### 4. Page Navigation
Below the script list, you'll find the pagination controls.

*   **Previous / Next Buttons:** Click these to move between pages of scripts. The buttons will become inactive if there are no more pages in that direction.
*   **Page Counter:** The `(current / total)` text in the middle shows your current position in the list.

[Image of the pagination controls]

#### 5. Action Buttons
At the very bottom of the GUI, there is a row of action buttons for core MQS functions.

[Image of the action buttons row]

From left to right:

*   **Refresh** (Icon: 🔄): Rescans the `my-qol-scripts/scripts/` folder. **Use this every time you add, remove, or update a script file while the game is running.** It will re-enable any scripts that were running before the refresh.
*   **Console** (Icon: 💻): Opens the [[The MQS Console|The-MQS-Console]], a powerful command-line interface for advanced control and debugging.
*   **All Off**: Immediately disables all currently running scripts. A quick way to turn everything off.
*   **More Options** (Icon: ⋮): Opens a new screen with more advanced options, including:
    *   [[Settings Screen|Settings-Screen]]
    *   [[Keybinds Screen|Keybinds-Screen]]
    *   Buttons to open your scripts and configs folders directly on your computer.
    *   A "Create Script" helper.

---

Now that you've mastered the main GUI, let's look at the in-game chat commands for more direct control.

**➡️ Next Step: [[In-Game Commands|In-Game-Commands]]**