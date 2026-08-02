const Minecraft = net.minecraft.client.Minecraft;

export function onEnable() {
  const fps = Minecraft.fps;
  console.log("Example Package enabled! MQP version: " + mqp.version);
  console.log("Current FPS: " + fps);
}

export function onDisable() {
  console.log("Example Package disabled!");
}
