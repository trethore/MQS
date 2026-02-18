const Component = net.minecraft.network.chat.Component;
// @module(main=TestCommand, name=Test Command Module, version=0.0.1)
class TestCommand {
    disposer = null;

    onEnable() {
        // triggered when the module is enabled
        println("Hello from Test Command Module!");
        const fakePlayerList = ["AwesomeDude", "CoolPlayer", "EpicGamer"];
        const rootBuilder = MQS.cmd.lit('greet', cmd => {
            cmd.run(ctx => {
                const message = "Please provide a name!";
                const player = MQS.utils.player();
                if (player) {
                    player.displayClientMessage(Component.literal(message), false);
                }
                println(message);
            });

            cmd.then(MQS.cmd.arg('name', MQS.cmd.types.word)
                .suggest(fakePlayerList)
                .run(ctx => {
                    const name = String(ctx.arg('name'));
                    const message = `Hello ${name}! Welcome!`;
                    const player = MQS.utils.player();
                    if (player) {
                        player.displayClientMessage(Component.literal(message), false);
                    }
                    println(message);
                }));
        });

        this.disposer = MQS.cmd.reg(rootBuilder);
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
