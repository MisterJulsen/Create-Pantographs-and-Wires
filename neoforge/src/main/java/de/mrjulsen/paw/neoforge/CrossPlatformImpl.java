package de.mrjulsen.paw.neoforge;

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
import net.neoforged.fml.config.ModConfig;

public final class CrossPlatformImpl {

    public static void registerConfig() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            PantographsAndWiresNeoForge.getModContainer().registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC, PantographsAndWires.MOD_ID + "-client.toml");
        }
        PantographsAndWiresNeoForge.getModContainer().registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC, PantographsAndWires.MOD_ID + "-common.toml");
        PantographsAndWiresNeoForge.getModContainer().registerConfig(ModConfig.Type.SERVER, ModServerConfig.SPEC, PantographsAndWires.MOD_ID + "-server.toml");
    }

    public static double interactionRange(Player player) {
        return 5;//player.getAttribute(NeoForgeMod..get()).getValue();
    }

    public static BlockEntity getClientContraptionBlockEntity(Contraption contraption, BlockPos localPos) {
        return contraption.getBlockEntityClientSide(localPos);
    }
}
