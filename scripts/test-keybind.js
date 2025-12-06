const Text = net.minecraft.text.Text;
const KEY_CODE = 66;
// @module(main=TestKeybind, name=Test Keybind Module, version=0.0.1)
class TestKeybind {

    disposer = null;
    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Keybind Module!");
        this.disposer = MQS.keybinds.bind(
            "test_keybind",
            KEY_CODE,
            this.onKeyPress.bind(this),
            MQS.keybinds.options().repeatable(true).build()
        );
    }

    onDisable() {
        // triggered when the module is disabled
        println("Goodbye from Test Keybind Module!");
        if (this.disposer) {
            this.disposer();
            this.disposer = null;
        }
    }

    onKeyPress() {
        const player = MQS.utils.mc.player();
        if (player) {
            player.sendMessage(Text.literal("Hello from Test Keybind!"), false);
        }
    }
}

exportModule(TestKeybind);
