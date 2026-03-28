package de.mrjulsen.paw.forge;

import com.simibubi.create.content.contraptions.Contraption;

import com.tterrag.registrate.builders.BlockBuilder;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.config.ModClientConfig;
import de.mrjulsen.paw.config.ModCommonConfig;
import de.mrjulsen.paw.config.ModServerConfig;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class CrossPlatformImpl {

    public static void registerConfig() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC, PantographsAndWires.MOD_ID + "-client.toml");
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC, PantographsAndWires.MOD_ID + "-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ModServerConfig.SPEC, PantographsAndWires.MOD_ID + "-server.toml");
    }

    public static double interactionRange(Player player) {
        return player.getAttribute(ForgeMod.BLOCK_REACH.get()).getValue();
    }

    public static BlockEntity getClientContraptionBlockEntity(Contraption contraption, BlockPos localPos) {
        return contraption.getBlockEntityClientSide(localPos);
    }
}
