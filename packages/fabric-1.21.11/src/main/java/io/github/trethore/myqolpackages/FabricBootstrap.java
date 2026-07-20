package io.github.trethore.myqolpackages;

import io.github.trethore.myqolpackages.api.Main;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricBootstrap implements ModInitializer {
  public static final String MOD_ID = "myqolpackages";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    Main.initialize();
    LOGGER.info("Initialized {}", MOD_ID);
  }
}
