const Minecraft = net.minecraft.client.Minecraft;

export function onEnable() {
  console.log("Example Package enabled! MQP version: " + mqp.version);
  console.log("Current FPS: " + Minecraft.fps);
  Minecraft.fps = 100;
  console.log("Current FPS: " + Minecraft.fps);
}

export function onDisable() {
  console.log("Example Package disabled!");
}
