package de.mrjulsen.wires.client;

import net.minecraft.client.Minecraft;

public class ClientUtils {
    
    public static float getMultiplierByGraphicsMode() {
        return switch (Minecraft.getInstance().options.graphicsMode().get()) {
			case FAST -> 0.5F;
			case FABULOUS -> 2F;
            default -> 1F;
		};
    }
}
