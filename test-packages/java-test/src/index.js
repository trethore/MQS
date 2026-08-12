const Minecraft = net.minecraft.client.Minecraft;
const Screen = net.minecraft.client.gui.screens.Screen;
const Button = net.minecraft.client.gui.components.Button;
const Component = net.minecraft.network.chat.Component;
const Runnable = packages.java.lang.Runnable;
const MqpJava = mqp.java;
const MqpType = MqpJava.type;
const MqpVisibility = MqpJava.visibility;

export function createJavaTestScreenClass() {
  return MqpJava
    .defineClass("JavaTestScreen")
    .extends(Screen)
    .implements(Runnable)
    .constructor({
      visibility: MqpVisibility.PUBLIC,
      implementation: function ($self, $super) {
        $super(Component.literal("Java Test"));
      },
    })
    .method({
      name: "init",
      returnType: MqpType.void,
      visibility: MqpVisibility.PUBLIC,
      override: true,
      implementation: function ($self, $super) {
        const button = Button.builder(Component.literal("Hello"), function () {
          console.log("hello");
        })
        .bounds($self.width / 2 - 75, $self.height / 2 - 10, 150, 20)
        .build();

        $super.addRenderableWidget(button);
      },
    })
    .method({
      name: "run",
      returnType: MqpType.void,
      visibility: MqpVisibility.PUBLIC,
      override: true,
      implementation: function () {
        console.log("running!");
      },
    })
    .build();
}

export function onEnable() {
  const JavaTestScreen = createJavaTestScreenClass();
  const screen = new JavaTestScreen();
  screen.run();
  const minecraft = Minecraft.getInstance();

  minecraft.execute(function () {
    if (minecraft.player !== null) {
      minecraft.setScreen(screen);
    }
  });
}

export function onDisable() {
  console.log("Java Test Package disabled!");
}
