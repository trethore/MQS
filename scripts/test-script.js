const Component = net.minecraft.network.chat.Component;
// @script(id=TestScript, name=Test Script, version=0.0.1)
class TestScript {
    onEnable() {
        // triggered when the script is enabled
        println("Hello from Test Script!");
        const player = MQS.utils.player();
        if (player) {
            player.displayClientMessage(Component.literal("Boom !"), false);
        }
    }

    onDisable() {
        // triggered when the script is disabled
        println("Goodbye from Test Script!");
    }
}

exportScript(TestScript);
