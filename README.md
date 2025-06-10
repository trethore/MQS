# My QOL Scripts - Minecraft Mod

## Description

This Minecraft Mod provides an api for running js scripts.
Its like JsMacros but way more powerful and flexible.

## Install

1. Download the latest release from the [releases page](https://github.com/trethore/MQS/releases).
2. Drop the mod inside the `mods` folder of your Minecraft instance.
3. Run Minecraft and execute scripts by placing your js files inside the scripts folder 
`.minecraft/my-qol-scripts/scripts/`.
4. Enjoy and dont foget to report issues this mod is in development and has no bugs (lie).

## Usage and Examples

### Using mappings in scripts

Classes from Minecraft can be imported with Yarn names using `importClass`:

```javascript
const Screen = importClass('net.minecraft.client.gui.screen.Screen');
```

Methods and fields are accessed with the same Yarn names. To subclass a
Minecraft class and override its methods so that the script works in both
development and production environments, use `Java.extendMapped`:

```javascript
const MyScreen = Java.extendMapped(Screen, {
  init: function () {
    Java.super(this).init();
    // your code here
  },
  customMethod: function () {
    // additional methods can be defined normally
  }
});

mc.send(() => mc.setScreen(new MyScreen()));
```

`Java.extendMapped` automatically translates Yarn method names to their
runtime equivalents, so the same script works whether the game is obfuscated
or not.

