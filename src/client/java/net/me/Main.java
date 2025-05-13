package net.me;

import net.fabricmc.api.ClientModInitializer;
import net.me.mappings.MappingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ClientModInitializer {
	public static final String MOD_ID = "my-qol-scripts";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String VERSION = "0.0.1";
	public static final String NAME = "My Qol Scripts";
	public static final String MC_VERSION = "1.21.4";

	@Override
	public void onInitializeClient() {
		MappingsManager.getInstance().init();
		System.out.println("MQS loaded succesfully !");

	}

}
