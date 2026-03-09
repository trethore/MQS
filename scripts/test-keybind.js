const KEY_CODE = 66; // B
// @script(main=TestKeybind, name=Test Keybind Module, version=0.0.1)
class TestKeybind {

    disposer = null;
    onEnable() {
        MQS.utils.chat.addInfoChatMessage("Hello from Test Keybind Module!", true);
        this.disposer = MQS.keybinds.bind(
            "test_keybind",
            KEY_CODE,
            this.onKeyPress.bind(this),
            MQS.keybinds.options().repeatable().build()
        );
    }

    onDisable() {
        MQS.utils.chat.addInfoChatMessage("Goodbye from Test Keybind Module!", true);
        if (this.disposer) {
            this.disposer();
            this.disposer = null;
        }
    }

    onKeyPress() {
        MQS.utils.chat.addInfoChatMessage("Hello from Test Keybind!", true);
    }
}

exportModule(TestKeybind);
