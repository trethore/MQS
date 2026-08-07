/*
 * My QOL Packages - Client-side Minecraft modding at runtime
 * Copyright (C) 2026 Titouan Réthoré
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.trethore.myqolpackages;

import io.github.trethore.myqolpackages.api.MqpRuntime;
import io.github.trethore.myqolpackages.api.MqpRuntimeEnvironment;
import io.github.trethore.myqolpackages.command.MqpClientCommand;
import java.nio.file.Path;
import java.util.Optional;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FabricBootstrap implements ClientModInitializer {
  public static final String MOD_ID = "myqolpackages";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitializeClient() {
    FabricLoader fabricLoader = FabricLoader.getInstance();
    Path mqpDirectory = fabricLoader.getGameDir().resolve(MOD_ID);
    String mqpVersion =
        fabricLoader
            .getModContainer(MOD_ID)
            .orElseThrow()
            .getMetadata()
            .getVersion()
            .getFriendlyString();
    MqpRuntimeEnvironment environment =
        new MqpRuntimeEnvironment(
            FabricBootstrap.class.getClassLoader(),
            Optional.empty(),
            Optional.of("assets/myqolpackages/mappings/client.txt"));
    MqpRuntime runtime = MqpRuntime.create(mqpDirectory, mqpVersion, environment);
    runtime.start();
    new MqpClientCommand(runtime).register();
    ClientTickEvents.END_CLIENT_TICK.register(client -> runtime.tick());
    ClientLifecycleEvents.CLIENT_STOPPING.register(client -> runtime.stop());
    LOGGER.info("Initialized {} with data directory {}", MOD_ID, mqpDirectory);
  }
}
