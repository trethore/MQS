const MinecraftClient = net.minecraft.client.Minecraft;
const Component = net.minecraft.network.chat.Component;
// @module(main=TestHookModule, name=Test Hook Module, version=0.0.1)
class TestHookModule {
    hookDisposer = null;

    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Hook Module!");
        MQS.utils.chat.addInfoChatMessage("Hello from Test Event Module!", true);

        this.hookDisposer = MQS.hooks.before(
            MinecraftClient,
            "setScreen",
            this.myHookCallback.bind(this)
        );
    }

    onDisable() {
        // triggered when the module is disabled
        println("Goodbye from Test Hook Module!");
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

exportModule(TestHookModule);
