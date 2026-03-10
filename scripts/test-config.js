const Component = net.minecraft.network.chat.Component;
// @script(id=TestConfigScript, name=Test Config Script, version=0.0.1)
class TestConfigScript {
    onEnable() {
        // triggered when the script is enabled
        println("Hello from Test Config Script!");
        const name = MQS.config.getString("name", "Toto");
        const player = MQS.utils.player();
        if (player) {
            const message = `Hello ${name}`;
            player.displayClientMessage(Component.literal(message), false);
        }
    }

    onDisable() {
        // triggered when the script is disabled
        println("Goodbye from Test Config Script!");
    }
}

exportScript(TestConfigScript);
