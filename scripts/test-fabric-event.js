const Text = net.minecraft.text.Text;
// @module(main=TestEvent, name=Test Fabric Event, version=0.0.1)
class TestEvent {
    disposer = null;
    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Event!");
        const player = MQS.utils.mc.player();
        if (player) {
            player.sendMessage(Text.literal("Hello from Test Module!"), false);
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
        const player = MQS.utils.mc.player();
        if (player) {
            player.sendMessage(Text.literal("Hello from Test Event!"), false);
        }
    }
}

exportModule(TestEvent);
