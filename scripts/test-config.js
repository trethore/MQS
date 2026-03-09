const Component = net.minecraft.network.chat.Component;
// @script(main=TestConfigModule, name=Test Config Module, version=0.0.1)
class TestConfigModule {
    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Config Module!");
        const name = MQS.config.getString("name", "Toto");
        const player = MQS.utils.player();
        if (player) {
            const message = `Hello ${name}`;
            player.displayClientMessage(Component.literal(message), false);
        }
    }

    onDisable() {
        // triggered when the module is disabled
        println("Goodbye from Test Config Module!");
    }
}

exportModule(TestConfigModule);
