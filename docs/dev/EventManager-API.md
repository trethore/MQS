The `EventManager` is your gateway to interacting with the game's flow. It allows you to "listen" for specific events—such as a game tick, a player action, or a rendering pass—and execute your own code when they happen.

MQS provides a unified `EventManager` that can handle its own custom events, a wide range of built-in **Fabric API events**, and a convenient `EventManager.Events` enum.

### Core Methods

The `EventManager` API is simple and consists of two main functions:

*   **`EventManager.register(EventType, callback)`**: Subscribes to an event.
*   **`EventManager.unregister(...)`**: Unsubscribes from an event.

#### `EventManager.register(EventType, callback)`

This function registers a listener for a specific event.

*   **`EventType`**: The event you want to listen to. This can be one of three things:
    1.  A member of the **`EventManager.Events` enum** (e.g., `EventManager.Events.EndClientTickEvent`). This is the easiest way to listen for MQS events.
    2.  A **Fabric Event Object**: A direct reference to a Fabric event, like `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK`.
    3.  An **MQS Event Class**: A class imported via `importClass()` that extends `net.me.event.Event` (less common now that `EventManager.Events` exists).
*   **`callback(...)`**: A JavaScript function that will be executed when the event occurs. The arguments it receives depend on the type of event (see examples below).

#### `EventManager.unregister(...)`

This function removes a listener that you previously registered. It's **crucial** to call this in your script's `onDisable()` method for every event you registered in `onEnable()` to prevent memory leaks and errors.

The `unregister` function has three variations:

1.  **`EventManager.unregister(EventType, callback)`**: Unregisters a *specific* callback for an event. The `callback` function must be the exact same function reference you used to register.
2.  **`EventManager.unregister(EventType)`**: Unregisters *all* callbacks for an event that belong to your script.
3.  **`EventManager.unregister()`**: Unregisters *all* callbacks for *all* events that belong to your script. This is a useful catch-all for your `onDisable()` method.

---

### Example 1: Using an MQS Event (`EventManager.Events`)

This example uses the convenient `EventManager.Events` object to listen for the end of a client tick and send a message. This is the recommended way to listen for MQS-specific events.

```javascript
// @module(main=TestEvent, name=Test MQS Event, version=0.0.1)

const Text = net.minecraft.text.Text;

class TestEvent {
    onEnable() {
        println("Hello from Test Event!");
        const player = MQS.utils.mc.player();
        if (player) {
            player.sendMessage(Text.literal("Hello from Test Module!"), false);
        }

        // Register using the Events enum and bind the 'this' context.
        EventManager.register(EventManager.Events.EndClientTickEvent, this.onTick.bind(this));
    }

    onDisable() {
        println("Goodbye from Test Event!");
        // Unregistering is crucial for cleanup.
        EventManager.unregister(EventManager.Events.EndClientTickEvent, this.onTick.bind(this));
    }

    // MQS events pass a single event object to the callback.
    onTick(event) {
        // We can get the client from the event object.
        const client = event.getClient();
        client.player.sendMessage(Text.literal("MQS Tick Event!"), false);
    }
}

exportModule(TestEvent);
```
> **Note on `.bind(this)`:** When passing a class method like `this.onTick` as a callback, its `this` context is lost. Using `.bind(this)` ensures that when `onTick` is called by the EventManager, `this` inside `onTick` correctly refers to your class instance so you can access its fields.

---

### Example 2: Using a Fabric API Event

This example uses a Fabric event directly to also listen for the end of a client tick. This approach is necessary for any event not provided by MQS.

```javascript
// @module(main=TestEvent, name=Test Fabric Event, version=0.0.1)

const Text = net.minecraft.text.Text;
// We get a reference to the Fabric event class.
const ClientTickEvents = net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

class TestEvent {
    onEnable() {
        println("Hello from Test Event!");
        const player = MQS.utils.mc.player();
        if (player) {
            player.sendMessage(Text.literal("Hello from Test Module!"), false);
        }

        // Register the listener on the specific event field (e.g., END_CLIENT_TICK).
        EventManager.register(ClientTickEvents.END_CLIENT_TICK, this.onTick.bind(this));
    }

    onDisable() {
        println("Goodbye from Test Event!");
        EventManager.unregister(ClientTickEvents.END_CLIENT_TICK, this.onTick.bind(this));
    }

    // Fabric events pass their arguments directly to the callback.
    // In this case, END_CLIENT_TICK provides the MinecraftClient instance.
    onTick(client) {
        client.player.sendMessage(Text.literal("Fabric Tick Event!"), false);
    }
}

exportModule(TestEvent);
```

### Available MQS Events (`EventManager.Events`)

MQS provides a set of its own simple events, accessible through the `EventManager.Events` object.

| Event Name | Description | Callback Properties |
|---|---|---|
| `StartClientTickEvent` | Fired at the beginning of a client tick. | `event.getClient()` |
| `EndClientTickEvent` | Fired at the end of a client tick. | `event.getClient()` |
| `MinecraftClientStopEvent`| Fired just before the game shuts down. | *(No properties)* |
| `ClientPacketInputEvent` | Fired when the client receives a packet from the server. | `event.getPacket()` |
| `ClientPacketOutputEvent`| Fired when the client sends a packet to the server. | `event.getPacket()` |
| `TitleEvent` | Fired when the client receives a main title packet. | *(No properties)* |
| `SubtitleEvent` | Fired when the client receives a subtitle packet. | *(No properties)* |

For most other scenarios, you should use the events provided by the **[Fabric API](https://fabricmc.net/wiki/tutorial:events)**.

---

With events covered, let's look at how to create your own commands.

**➡️ Next Step: [[CommandManager API|CommandManager-API]]**
