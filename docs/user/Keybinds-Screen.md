# Keybinds Screen

The Keybinds screen is where you can view and customize all the keybinds registered by your active scripts. This allows you to resolve conflicts and set up your controls exactly how you like them.

You can access this screen from the "More Options" menu in the main `/mqs` GUI or by running the command `/mqs screen keybinds`.

[Image of the Keybinds Screen]

---

### How to Rebind a Key

The screen lists keybinds grouped by the script that owns them.

1.  **Find the Keybind:** Scroll through the list to find the keybind you wish to change.
2.  **Click the Button:** Click the button on the right that shows the current key. The text will change to `> ... <`, indicating that MQS is now listening for your input.
3.  **Press the New Key:** Press any key on your keyboard or any button on your mouse. This will become the new binding.
    *   To **unbind** a key, press the `Escape` key while it is listening. The keybind will then show as "UNBOUND".

Your changes are saved automatically and will persist between game sessions.

### For Developers

If you are a script developer, you can add your own keybinds to this screen using the [[KeybindManager API|KeybindManager-API]].