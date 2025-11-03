const Text = net.minecraft.text.Text;
// @module(main=TestCommand, name=Test Command Module, version=0.0.1)
class TestCommand {
    disposer = null;

    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Command Module!");
        const fakePlayerList = ["AwesomeDude", "CoolPlayer", "EpicGamer"];
        const rootBuilder = MQS.commands.literal('greet', builder => {
            builder.executes(ctx => {
                const message = "Please provide a name!";
                const player = MQS.utils.mc.player();
                if (player) {
                    player.sendMessage(Text.literal(message), false);
                }
                println(message);
            });

            builder.then(MQS.commands.argument('name', 'WORD')
                .suggests(fakePlayerList)
                .executes(ctx => {
                    const name = ctx.getArgumentAsString('name');
                    const message = `Hello ${name}! Welcome!`;
                    const player = MQS.utils.mc.player();
                    if (player) {
                        player.sendMessage(Text.literal(message), false);
                    }
                    println(message);
                }));
        });

        this.disposer = MQS.commands.register(rootBuilder);
    }

    onDisable() {
        // triggered when the module is disabled
        println("Goodbye from Test Command Module!");
        if (this.disposer) {
            this.disposer();
            this.disposer = null;
        }
    }
}

exportModule(TestCommand);
