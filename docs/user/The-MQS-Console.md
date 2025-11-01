The MQS Console is a powerful, in-game command-line interface designed for advanced script management, debugging, and diagnostics. It provides more detailed logging than the standard chat and has its own set of specialized commands.

### How to Open the Console

You can open the console screen in two ways:
1.  **From the Main GUI:** Click the "Console" button (💻 icon) in the main `/mqs` menu.
2.  **Via Chat Command:** Type `/mqs screen console` in chat.

[Image of the Console Screen]

### Console Features

*   **Live Log Tailing:** The console automatically scrolls to show the latest messages. You can stop this by scrolling up manually.
*   **Color-Coded Output:** Messages are colored based on their type (Info, Success, Error, Command) for easy readability.
*   **Command History:** Use the `Up Arrow` and `Down Arrow` keys to navigate through previously executed commands.
*   **Rich Output:** Unlike chat, the console can display multi-line output from commands like `help` and `list`.

---

### Console-Specific Commands

The console has a unique set of commands that are not available in the regular chat. Type `help` in the console to see this list at any time.

*Note: Some commands (like `list` or `enable`) behave similarly to their `/mqs script` counterparts, but are designed for the console's interface.*

| Command | Usage | Description |
|---|---|---|
| `help` | `help` | Shows a list of all available console commands, their descriptions, and usage. |
| `clear` | `clear` | Clears all messages from the console screen. |
| `copytail` | `copytail <number=10>` | Copies the last `n` lines of console output to your clipboard. Defaults to 10 lines if no number is given. |
| `list` | `list` | Lists all available scripts and their status, without Minecraft's formatting codes. |
| `enable` | `enable <script_id>` | Enables a script. Supports script IDs with spaces if you wrap them in quotes (e.g., `enable "my script.js:My Script"`). |
| `disable` | `disable <script_id>` | Disables a running script. |
| `disableall` | `disableall` | Disables all currently running scripts. |
| `refresh` | `refresh` | Refreshes the script list from disk, disabling all running scripts. |
| `refreshandreenable` | `refreshandreenable` | Refreshes scripts and re-enables previously running ones. |
| `saveconfig` | `saveconfig <script_id>` | Saves the configuration for a specific running script. |
| `saveconfigs` | `saveconfigs` | Saves the configurations for all currently running scripts. |
| `logredirect` | `logredirect <true/false>` | Redirects Minecraft's internal logs (System.out, System.err, and SLF4J) to the MQS console. This is an **incredibly useful tool for script developers** to see detailed output and error messages from their scripts. |
| `allowallclasses` | `allowallclasses <true/false>` | **[DANGEROUS]** Toggles whether scripts are allowed to access all Java classes. By default, access is restricted for security. Only enable this if you fully trust every script you are running. A script reload (`refreshandreenable`) is required for this to take full effect. |

---

With the console mastered, let's look at how MQS is configured.

**➡️ Next Step: [[Configuration|Configuration]]**