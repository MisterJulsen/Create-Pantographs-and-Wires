package de.mrjulsen.paw.fabric;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.content.contraptions.Contraption;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.config.ModClientConfig;
import de.mrjulsen.paw.config.ModCommonConfig;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.mixin.ContraptionAccessor;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import fuzs.forgeconfigapiport.impl.config.ForgeConfigRegistryImpl;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CrossPlatformImpl {

    public static void registerConfig() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            ForgeConfigRegistryImpl.INSTANCE.register(PantographsAndWires.MOD_ID, ModConfig.Type.CLIENT, ModClientConfig.SPEC, PantographsAndWires.MOD_ID + "-client.toml");
        }
        ForgeConfigRegistryImpl.INSTANCE.register(PantographsAndWires.MOD_ID, ModConfig.Type.COMMON, ModCommonConfig.SPEC, PantographsAndWires.MOD_ID + "-common.toml");
        ForgeConfigRegistryImpl.INSTANCE.register(PantographsAndWires.MOD_ID, ModConfig.Type.SERVER, ModServerConfig.SPEC, PantographsAndWires.MOD_ID + "-server.toml");
    }

    public static double interactionRange(Player player) {
        throw new AssertionError();
    }
    
    public static double reach(Player p) {
		return ReachEntityAttributes.getReachDistance(p, p.isCreative() ? 5 : 4.5);
	}    

    public static BlockEntity getClientContraptionBlockEntity(Contraption contraption, BlockPos localPos) {
        var maybeNullClientContraption = ((ContraptionAccessor)contraption).paw$clientContraption().getAcquire();
        if (maybeNullClientContraption == null) {
            return null;
        }
        return maybeNullClientContraption.getBlockEntity(localPos);
    }  
}
