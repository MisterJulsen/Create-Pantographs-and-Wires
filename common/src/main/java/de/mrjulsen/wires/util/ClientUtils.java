package de.mrjulsen.wires.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public class ClientUtils {
    
    public static float getMultiplierByGraphicsMode() {
        return switch (Minecraft.getInstance().options.graphicsMode().get()) {
			case FAST -> 0.5F;
			case FABULOUS -> 2F;
            default -> 1F;
		};
    }

    public static Level level() {
        return Minecraft.getInstance().level;
    }
}
