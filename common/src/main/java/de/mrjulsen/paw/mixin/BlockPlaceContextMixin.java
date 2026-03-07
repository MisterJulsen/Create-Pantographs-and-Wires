package de.mrjulsen.paw.mixin;

import org.spongepowered.asm.mixin.Mixin;

import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockPlaceContext.class)
public class BlockPlaceContextMixin implements BlockPlaceContextExtension { 
    @Unique
    private BlockPos placedOnPos;
    @Unique
    private BlockState placedOnState;

    @Override
    public void paw$setPlacedOnPos(BlockPos placedOnPos) {
        this.placedOnPos = placedOnPos;
    }

    @Override
    public void paw$setPlacedOnState(BlockState placedOnState) {
        this.placedOnState = placedOnState;
    }

    @Override
    public BlockPos paw$getPlacedOnPos() {
        return placedOnPos;
    }

    @Override
    public BlockState paw$getPlacedOnState() {
        return placedOnState;
    }    
}
