This page provides a collection of practical examples and recipes to help you get started with writing your own scripts. Each example is a complete, working script that demonstrates how to use one or more MQS APIs to achieve a specific goal.

You can copy and paste any of these examples into a new `.js` file in your `scripts` folder to try them out.

### 1. Simple Chat Command: Player Coordinates

**Goal:** Create a `/coords` command that prints the player's current coordinates to the chat.

**APIs Used:** `CommandManager`, `MQSUtils.Chat`

```javascript
// @module(main='CoordsCommand', name='Coords Command', version='1.0.0')

class CoordsCommand {
    onEnable() {
        const cmd = CommandManager.literal('coords')
            .executes(ctx => {
                const player = ctx.getSource().getPlayer();
                if (!player) {
                    MQSUtils.Chat.addErrorChatMessage("Player not found!");
                    return;
                }
                const pos = player.getPos();
                const x = pos.getX().toFixed(2);
                const y = pos.getY().toFixed(2);
                const z = pos.getZ().toFixed(2);

                MQSUtils.Chat.addInfoChatMessage(`Your coordinates: ${x}, ${y}, ${z}`);
            });

        CommandManager.register(cmd);
    }

    onDisable() {
        CommandManager.unregister('coords');
    }
}

exportModule(CoordsCommand);
```

### 2. Simple ESP: Pig Highlighter

**Goal:** Draw a green box around all nearby pigs.

**APIs Used:** `EventManager`, `MQSUtils.Render3D`

```javascript
// @module(main='PigESP', name='Pig ESP', version='1.0.0')

const WorldRenderEvents = net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
const PigEntity = net.minecraft.entity.passive.PigEntity;
const Color = java.awt.Color;

class PigESP {
    onRender = (context) => {
        const world = MQSUtils.Mc.getWorld().get();
        const player = MQSUtils.Mc.getPlayer().get();
        if(!world || !player) return;

        const buffer = MQSUtils.Render3D.setupRender(
            net.minecraft.client.gl.ShaderProgramKeys.getRenderTypeLines(),
            net.minecraft.client.render.VertexFormat.DrawMode.LINES,
            net.minecraft.client.render.VertexFormats.LINES,
            true // espMode
        );

        for (const entity of world.getEntities()) {
            if (entity instanceof PigEntity && entity.isAlive()) {
                const box = entity.getBoundingBox();
                MQSUtils.Render3D.drawBox(context, buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, Color.GREEN, true);
            }
        }

        MQSUtils.Render3D.endRender(buffer);
    };

    onEnable() {
        EventManager.register(WorldRenderEvents.AFTER_ENTITIES, this.onRender);
    }

    onDisable() {
        EventManager.unregister(WorldRenderEvents.AFTER_ENTITIES, this.onRender);
    }
}

exportModule(PigESP);
```

### 3. Keybind Toggled Feature with Config

**Goal:** Create a keybind (`V`) that toggles a "fly boost" feature. The boost amount and whether the feature is active should be saved to the config.

**APIs Used:** `KeybindManager`, `ConfigManager`, `EventManager`

```javascript
// @module(main='FlyBoost', name='Fly Boost', version='1.0.0')

const KEY_V = 86; // GLFW Key code for 'V'

class FlyBoost {
    // Load settings from config, with default values
    isBoostEnabled = ConfigManager.get('boostEnabled', false);
    boostMultiplier = ConfigManager.get('boostMultiplier', 3.0);

    onToggle = () => {
        this.isBoostEnabled = !this.isBoostEnabled;
        ConfigManager.set('boostEnabled', this.isBoostEnabled); // Save setting
        MQSUtils.Chat.addInfoChatMessage(`Fly Boost is now ${this.isBoostEnabled ? 'ON' : 'OFF'}.`);
    };

    onTick = (event) => {
        const player = event.getClient().player;
        if (this.isBoostEnabled && player && player.getAbilities().flying && event.getClient().options.sprintKey.isPressed()) {
            const lookVec = player.getRotationVector();
            player.setVelocity(
                lookVec.x * this.boostMultiplier,
                lookVec.y * this.boostMultiplier,
                lookVec.z * this.boostMultiplier
            );
        }
    };

    onEnable() {
        KeybindManager.register('Toggle Fly Boost', KEY_V, this.onToggle);
        EventManager.register(EventManager.Events.StartClientTickEvent, this.onTick);
    }

    onDisable() {
        KeybindManager.unregisterAll();
        EventManager.unregisterAll();
    }
}

exportModule(FlyBoost);
```

### 4. Advanced Hooking: Armor Durability Warning

**Goal:** Hook the `damage` method on `ItemStack` to warn the player when their armor takes damage and is below 10% durability.

**APIs Used:** `HookManager`, `MQSUtils.Chat`, `wrap`

```javascript
// @module(main='ArmorWarning', name='Armor Durability Warning', version='1.0.0')

const ItemStack = net.minecraft.item.ItemStack;

class ArmorWarning {
    damageHook = (method, args, chain) => {
        // We must wrap 'this' to get a proxy that understands Yarn mappings.
        // In a hook, 'this' refers to the instance of the hooked class (ItemStack).
        const itemStack = wrap(this);

        if (!itemStack.isDamageable()) {
            return chain(...args);
        }

        const durabilityLeft = itemStack.getMaxDamage() - itemStack.getDamage();
        const damageToApply = args[0];

        if (durabilityLeft - damageToApply < itemStack.getMaxDamage() * 0.1) {
            const itemName = itemStack.getName().getString();
            MQSUtils.Chat.addWarnChatMessage(`${itemName} is about to break!`);
        }

        return chain(...args);
    };

    onEnable() {
        HookManager.hook(ItemStack, 'damage', this.damageHook);
    }

    onDisable() {
        HookManager.unhookAll();
    }
}

exportModule(ArmorWarning);
```

### 5. Advanced Interop: Creating a Custom GUI Screen

**Goal:** Use `extendMapped` to create a new, custom GUI screen class that extends Minecraft's `Screen` and opens when the script is enabled.

**APIs Used:** `extendMapped`, `importClass`

```javascript
// @module(main=TestExtends, name=Test Extends Module, version=0.0.1)

const MinecraftClient = net.minecraft.client.MinecraftClient;
const Text = net.minecraft.text.Text;
const Screen = net.minecraft.client.gui.screen.Screen;

// A factory function to create our custom screen
function createCustomScreen(name) {
    const CustomScreen = extendMapped({
        extends: Screen
    }, {
        init: function () {
            this._super.init(); // Call parent method
            println("Custom screen initialized: " + name);
        },
        open: function () {
            const mc = MinecraftClient.getInstance();
            mc.send(() => mc.setScreen(this._self)); // Use _self to pass raw instance
        }
    });
    // Instantiate with parent constructor args (Screen takes a Text title)
    return new CustomScreen(Text.literal(name));
}

class TestExtends {
    onEnable() {
        const mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            println("Player is null, cannot open screen.");
            return;
        }
        const customScreen = createCustomScreen("My Custom Screen");
        customScreen.open();
    }

    onDisable() {
        // No cleanup needed, screen will be closed by the game.
    }
}

exportModule(TestExtends);
```

This concludes the MQS Wiki documentation! These examples should provide a solid starting point for developers to build their own amazing scripts.