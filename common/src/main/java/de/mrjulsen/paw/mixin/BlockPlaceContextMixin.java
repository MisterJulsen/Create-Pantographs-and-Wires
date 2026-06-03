package de.mrjulsen.paw.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockPlaceContext.class)
public class BlockPlaceContextMixin implements BlockPlaceContextExtension {

    @Override
    public BlockPos paw$getPlacedOnPos() {
        BlockPlaceContext self = (BlockPlaceContext)(Object)this;
        return self.getClickedPos().relative(self.getClickedFace().getOpposite());
    }

    @Override
    public BlockState paw$getPlacedOnState() {
        BlockPlaceContext self = (BlockPlaceContext)(Object)this;
        Level level = self.getLevel();
        BlockPos clickedBlockPos = self.getClickedPos().relative(self.getClickedFace().getOpposite());
        return level.getBlockState(clickedBlockPos);
    }    
}
