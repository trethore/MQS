// @script(id=TestEvent, name=Test MQS Event, version=0.0.1)
class TestEvent {
    disposer = null;
    onEnable() {
        // triggered when the script is enabled
        println("Hello from Test Event!");
        MQS.utils.chat.addInfoChatMessage("Hello from Test Event Script!", true);
        this.disposer = MQS.events.onEndClientTick(this.onTick.bind(this));
    }

    onDisable() {
        // triggered when the script is disabled
        println("Goodbye from Test Event!");
        if (this.disposer) {
            this.disposer();
            this.disposer = null;
        }
    }

    onTick(event) { // Mqs events return the event instance
        const player = MQS.utils.player();
        if (player) {
           MQS.utils.chat.addInfoChatMessage("Hello from Test Event!", true);
        }
    }
}

exportScript(TestEvent);
