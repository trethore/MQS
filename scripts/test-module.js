const Component = net.minecraft.network.chat.Component;
// @script(id=TestModule, name=Test Module, version=0.0.1)
class TestModule {
    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Module!");
        const player = MQS.utils.player();
        if (player) {
            player.displayClientMessage(Component.literal("Boom !"), false);
        }
    }

    onDisable() {
        // triggered when the module is disabled
        println("Goodbye from Test Module!");
    }
}

exportModule(TestModule);
