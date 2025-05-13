package net.me;

import net.fabricmc.api.ClientModInitializer;

public class Main implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		System.out.println("MQS loaded succesfully !");
	}

}
