const Minecraft = importClass("Minecraft");
// @script(id=TestHookScript, name=Test Hook Script, version=0.0.1)
class TestHookScript {
    hookDisposer = null;

    onEnable() {
        // triggered when the script is enabled
        println("Hello from Test Hook Script!");
        MQS.utils.chat.addInfoChatMessage("Hello from Test Event Script!", true);

        this.hookDisposer = MQS.hooks.before(
            Minecraft,
            "setScreen",
            this.myHookCallback.bind(this)
        );
    }

    onDisable() {
        // triggered when the script is disabled
        println("Goodbye from Test Hook Script!");
        if (this.hookDisposer) {
            this.hookDisposer();
            this.hookDisposer = null;
        }
    }

    myHookCallback(context, args, next) {
        const screen = args[0];

        if (screen) {
           println("Screen opened: " + screen.getTitle().getString());
        } else {
           println("Screen closed (setScreen was called with null).");
        }

        return next(...args)
    }
}

exportScript(TestHookScript);
