`MQSUtils` is a global object that provides a collection of utility libraries for common scripting tasks. It helps simplify actions like rendering, sending chat messages, and getting information about the game state, so you don't have to write boilerplate code.

The `MQSUtils` object is structured as a container for other specialized utility objects.

### Accessing Utilities

You access each utility as a property of the main `MQSUtils` object. For example:

```javascript
// To send a chat message:
MQSUtils.Chat.addInfoChatMessage("Hello from MQSUtils!");

// To get the camera's position:
const camPos = MQSUtils.Camera.getCameraPos();
```

---

### Available Utility Libraries

Here is a summary of all the utility objects available under `MQSUtils`.

#### `MQSUtils.Chat`
Provides functions for interacting with the player's chat. Based on `ChatUtils.java`.
*   `addInfoChatMessage(message, [prefix=true])`: Shows a client-side message in chat with neutral formatting.
*   `addSuccessChatMessage(message, [prefix=true])`: Shows a message in green.
*   `addWarnChatMessage(message, [prefix=true])`: Shows a message in gold.
*   `addErrorChatMessage(message, [prefix=true])`: Shows a message in red.
*   `addRawMessage(message)`: Shows a message with no prefix or special coloring.
*   `sendChatMessage(message)`: Sends a message to the server as if the player typed it.
*   `sendChatCommand(command)`: Executes a command (e.g., `/home`).

#### `MQSUtils.Render2D`
Provides functions for drawing simple 2D shapes on the screen. This is typically used inside a render event. Based on `Render2DUtils.java`.
*   `drawRect(context, x, y, width, height, color)`
*   `drawOutline(context, x, y, width, height, lineWidth, color)`
*   `drawRoundedRect(context, x, y, width, height, radius, quality, color)`
*   `drawRoundedOutline(context, x, y, width, height, radius, lineWidth, quality, color)`
*   `drawImage(identifier, x1, y1, x2, y2, rotation, parity, color)`

#### `MQSUtils.TextRender`
Provides functions for drawing text on the screen. It uses a custom font for better readability and scaling. Based on `TextRenderUtils.java`.
*   `drawCustomText(context, text, x, y, color, shadow, scale)`
*   `drawCustomCenteredText(context, text, x, y, color, shadow, scale)`

#### `MQSUtils.Render3D`
Provides functions for drawing simple 3D shapes in the world. Based on `Render3DUtils.java`. **Usage Note:** You must use `setupRender` before drawing and `endRender` after. See the example in [[Examples & Recipes|Examples-and-Recipes]].
*   `setupRender(shader, drawMode, vertexFormat, espMode)`: Prepares the rendering pipeline.
*   `endRender(bufferBuilder)`: Finalizes the rendering.
*   `drawLine(context, bufferBuilder, ...)`
*   `drawBox(context, bufferBuilder, ...)`
*   `getInterpolatedBoundingBox(livingEntity, partialTicks)`

#### `MQSUtils.Mc`
A small utility for conveniently getting core Minecraft client instances. Based on `McUtils.java`.
*   `getMc()`: Returns an `Optional<MinecraftClient>`.
*   `getPlayer()`: Returns an `Optional<ClientPlayerEntity>`.
*   `getWorld()`: Returns an `Optional<ClientWorld>`.
*   *(Note: Since you are in JavaScript, you'll need to handle the `Optional` by checking `isPresent()` and then calling `get()`)*

#### `MQSUtils.Camera`
A utility for getting information about the camera's position. Based on `CameraUtils.java`.
*   `getCameraPos()`: Returns the camera's `Vec3d` position.
*   `getCameraBlockPos()`: Returns the camera's `BlockPos`.

#### `MQSUtils.Color`
A utility for color manipulation. Based on `ColorUtils.java`.
*   `getRainbowColor(speed, saturation, brightness)`: Returns an integer color value that cycles through the rainbow.

---

### Full Example: Simple HUD

This script uses several utilities to draw a simple Heads-Up Display (HUD) in the top-left corner of the screen, showing the player's coordinates.

```javascript
// @module(main='UtilsDemo', name='MQSUtils Demo', version='1.0.0')

// Import the Fabric event for HUD rendering.
const HudRenderCallback = net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
// Import the Java Color class for easy color creation.
const Color = java.awt.Color;

class UtilsDemo {
    // The HUD render event provides the DrawContext and a RenderTickCounter.
    onRenderHud = (drawContext, tickCounter) => {
        // Use McUtils to make sure we're in-game.
        if (!MQSUtils.Mc.getPlayer().isPresent()) {
            return;
        }

        // Draw a semi-transparent background rectangle using Render2D.
        MQSUtils.Render2D.drawRect(drawContext, 10, 10, 150, 40, new Color(0, 0, 0, 128).getRGB());

        // Draw a border around it.
        MQSUtils.Render2D.drawOutline(drawContext, 10, 10, 150, 40, 1, Color.CYAN.getRGB());

        // Use TextRender to draw a title for our HUD.
        MQSUtils.TextRender.drawCustomText(drawContext, "MQSUtils Demo!", 15, 15, Color.WHITE.getRGB(), true, 1.0);

        // Use Camera to get player position and display it.
        const camPos = MQSUtils.Camera.getCameraPos();
        const posText = `Pos: ${camPos.getX().toFixed(1)}, ${camPos.getY().toFixed(1)}, ${camPos.getZ().toFixed(1)}`;
        MQSUtils.TextRender.drawCustomText(drawContext, posText, 15, 30, Color.YELLOW.getRGB(), true, 0.8);
    };

    onEnable() {
        EventManager.register(HudRenderCallback.EVENT, this.onRenderHud);
    }

    onDisable() {
        EventManager.unregister(HudRenderCallback.EVENT, this.onRenderHud);
    }
}

exportModule(UtilsDemo);

```

---

This concludes the overview of the main MQS APIs. The final developer guide page will cover Java Interoperability in more detail.

**➡️ Next Step: [[Java Interoperability|Java-Interoperability]]**
