package de.mrjulsen.paw.block.extended;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockPlaceContextExtension {
    BlockPos paw$getPlacedOnPos();
    BlockState paw$getPlacedOnState();
    void paw$setPlacedOnPos(BlockPos pos);
    void paw$setPlacedOnState(BlockState state);
}
