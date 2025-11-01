Welcome to the MQS Scripting Guide! This page will walk you through the process of creating a simple "Hello, World!" script. By the end, you'll understand the fundamental structure of an MQS script and how to make it interact with the game.

### Prerequisites

Before you start, make sure you have:
1.  A good text editor. [Visual Studio Code](https://code.visualstudio.com/) is highly recommended as it has excellent JavaScript support.
2.  Completed the [[Installation Guide|../user/Installation]] and know where your `scripts` folder is.

---

### The Core Concepts

Every MQS script is built on two core concepts: the **`@module` annotation** and the **exported main class**.

#### 1. The `@module` Annotation

This is a special comment at the top of your script file that tells MQS how to load your script. It's not code, but MQS reads it to get metadata.

```javascript
// @module(main='MyScript', name='My Awesome Script', version='1.0.0')
```

*   `main`: The name of the JavaScript class that MQS should use as the entry point for this module. This is **required**.
*   `name`: The human-readable name that will be displayed in the MQS GUI. This is **required**.
*   `version`: The version of your script. This is optional but highly recommended.

A single `.js` file can contain multiple `@module` annotations, each defining a separate, independent script.

#### 2. The Exported Main Class

The class you specified in the `main` parameter must be defined and then "exported" so MQS can find and instantiate it.

This class will contain special lifecycle methods that MQS calls automatically:
*   `onEnable()`: This method is called once when the script is enabled. It's the perfect place to set up event listeners, register commands, or run initial setup code.
*   `onDisable()`: This method is called once when the script is disabled. You should use this to clean up everything you set up in `onEnable()` to prevent memory leaks and unwanted behavior.

---

### Your First Script: Hello, World!

Let's create a script that prints "Hello, Scripting World!" to the chat when it's enabled.

#### Step 1: Create the File

1.  Navigate to your `my-qol-scripts/scripts/` folder.
2.  Create a new file named `hello-world.js`.

#### Step 2: Write the Code

Open `hello-world.js` in your text editor and paste the following code:

```javascript
// @module(main='HelloWorld', name='Hello World Script', version='1.0.0')

// We can directly access many of Minecraft's classes.
const MinecraftClient = net.minecraft.client.MinecraftClient;
const Text = net.minecraft.text.Text;

class HelloWorld {
    /**
     * This method is called when the script is enabled in the MQS menu.
     */
    onEnable() {
        // Get the current Minecraft client instance
        const mc = MinecraftClient.getInstance();

        // Send a message directly to the player's chat
        mc.player.sendMessage(Text.literal("Hello, Scripting World!"), false);

        // Also print a message to the game's log/console for debugging
        println("Hello World script enabled.");
    }

    /**
     * This method is called when the script is disabled.
     */
    onDisable() {
        println("Hello World script disabled.");
    }
}

// This line makes the HelloWorld class available for MQS to load.
exportModule(HelloWorld);
```

#### Code Breakdown:
*   `@module(...)`: This annotation tells MQS that this file contains a script named "Hello World Script" whose main class is `HelloWorld`.
*   `const MinecraftClient = ...`: We are getting a direct reference to Minecraft's core classes. `net.minecraft...` is globally available.
*   `onEnable()`: When the script is turned on, this code runs. It gets the game instance (`mc`) and uses it to send a `Text` object to the player's chat.
*   `onDisable()`: When the script is turned off, this code simply prints a message to the game's log.
*   `exportModule(HelloWorld)`: This makes our `HelloWorld` class discoverable by MQS.

#### Step 3: See It in Action

1.  Save the `hello-world.js` file.
2.  Launch Minecraft or, if it's already running, use the **Refresh** button in the `/mqs` GUI.
3.  Open the `/mqs` GUI. You should now see "Hello World Script" in the list.
4.  Enable the script by clicking its toggle.

You should immediately see `Hello, Scripting World!` appear in your chat.

Congratulations, you've created and run your first MQS script!

---

Now that you understand the basics, let's explore the scripting environment in more detail.

**➡️ Next Step: [[The Scripting Environment|The-Scripting-Environment]]**