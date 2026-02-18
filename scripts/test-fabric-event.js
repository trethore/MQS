const Component = net.minecraft.network.chat.Component;
// @module(main=TestEvent, name=Test Fabric Event, version=0.0.1)
class TestEvent {
    disposer = null;
    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Event!");
        const player = MQS.utils.player();
        if (player) {
            const message = "Hello from Test Module!";
            player.displayClientMessage(Component.literal(message), false);
        }
        this.disposer = MQS.events.fabric.clientTickEnd(this.onTick.bind(this));
    }

    onDisable() {
        // triggered when the module is disabled
        println("Goodbye from Test Event!");
        if (this.disposer) {
            this.disposer();
            this.disposer = null;
        }
    }

    onTick(client) { // ClientTickEvents.END_CLIENT_TICK return the client instance
        const player = MQS.utils.player();
        if (player) {
            const message = "Hello from Test Event!";
            player.displayClientMessage(Component.literal(message), false);
        }
    }
}

exportModule(TestEvent);
