const Text = net.minecraft.text.Text;
// @module(main=TestConfigModule, name=Test Config Module, version=0.0.1)
class TestConfigModule {
    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Config Module!");
        const name = MQS.config.getString("name", "Toto");
        //MQS.config.set("name", "Tata");
        const player = MQS.utils.mc.player();
        if (player) {
            player.sendMessage(Text.literal(`Hello ${name}`), false);
        }
    }

    onDisable() {
        // triggered when the module is disabled
        println("Goodbye from Test Config Module!");
    }
}

exportModule(TestConfigModule);
