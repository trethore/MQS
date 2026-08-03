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

  fetch("https://geocoding-api.open-meteo.com/v1/search?name=Paris&count=1")
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        return response.json();
      })
      .then((content) => {
        const location = content.results[0];
        const lat = location.latitude;
        const long = location.longitude;
        console.log(`Latitude: ${lat}, Longitude: ${long}`);
      })
      .catch((error) => {
        console.error(error);
      });
}

export function onDisable() {
  console.log("Example Package disabled!");
}
