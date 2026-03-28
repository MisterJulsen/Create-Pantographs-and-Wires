package de.mrjulsen.paw;

import com.simibubi.create.content.contraptions.Contraption;

import com.tterrag.registrate.builders.BlockBuilder;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CrossPlatform {
        
    @ExpectPlatform
    public static void registerConfig() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static double interactionRange(Player player) {
        throw new AssertionError();
    }     

    @ExpectPlatform
    public static BlockEntity getClientContraptionBlockEntity(Contraption contraption, BlockPos localPos) {
        throw new AssertionError();
    }
}
