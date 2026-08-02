const Minecraft = net.minecraft.client.Minecraft;
const BlockPos = net.minecraft.core.BlockPos;

export function onEnable() {
  console.log("Example Package enabled! MQP version: " + mqp.version);
  console.log("Current FPS: " + Minecraft.fps);
  Minecraft.fps = 100;
  console.log("Current FPS: " + Minecraft.fps);

  const blockPos1 = new BlockPos(1, 2, 3);
  console.log("Block Position 1: " + blockPos1);

  const blockPos2 = new BlockPos(3, 2, 1);
  console.log("Block Position 2: " + blockPos2);

  console.log("Are the 2 BlockPos equal? " + blockPos1._equals(blockPos2));
  console.log("Is blockPos1 an instance of BlockPos? " + blockPos1._instanceof(BlockPos));
}

export function onDisable() {
  console.log("Example Package disabled!");
}
