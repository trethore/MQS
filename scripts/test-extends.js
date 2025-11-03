const Text = net.minecraft.text.Text;
const Screen = net.minecraft.client.gui.screen.Screen;
const Runnable = importClass("java.lang.Runnable");

function createCustomScreen(name) {
    // Step 1: Define the class with its implementation
    const CustomScreen = extendMapped({
        extends: Screen,
        implements: Runnable
    }, {
        // This is an override
        init: function () {
            // 'this' refers to the new instance. _super calls the parent's init.
            this._super.init();
            println("Custom screen initialized: " + name);
        },
        run: function () {
            println("Custom screen running: " + name);
        },

        // This is an addon method
        open: function () {
            const mc = MQS.utils.mc.client();
            if (mc) {
                mc.send(() => {
                    mc.setScreen(this);
                    if (customScreen.equals(mc.currentScreen)) {
                        println("equals is true");
                    }
                });
            }
            println("Person: " + this.data.name);
            this.run();
        },
        test: function () {
            println("Custom screen test: " + name);
        },

        // This is an addon property
        data: {
            name: "Joe",
            age: "30"
        }
    });

    // Step 2: Instantiate the class, passing only constructor arguments
    const customScreen = new CustomScreen(Text.literal(name));

    return customScreen;
}


// @module(main=TestExtends, name=Test Extends Module, version=0.0.1)
class TestExtends {
    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Extends Module!");
        const mc = MQS.utils.mc.client();
        if (!mc || mc.player == null) {
            println("Player is null, try loading the script later");
            return;
        }
        mc.player.sendMessage(Text.literal("Hello from Test Extends Module!"), false);
        const customScreen = createCustomScreen("My Custom Screen");
        customScreen.open();
        if (customScreen._instanceof(Runnable)) {
            println("isInstanceOf is true");
        }


    }

    onDisable() {
        // triggered when the module is disabled
        println("Goodbye from Test Extends Module!");
    }
}

exportModule(TestExtends);
