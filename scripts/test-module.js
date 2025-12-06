const Text = net.minecraft.text.Text;
// @module(main=TestModule, name=Test Module, version=0.0.1)
class TestModule {
    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Module!");
        const player = MQS.utils.mc.player();
        if (player) {
            player.sendMessage(Text.literal("Hello from Test Module!"), false);
        }
    }

    onDisable() {
        // triggered when the module is disabled
        println("Goodbye from Test Module!");
    }
}

exportModule(TestModule);
