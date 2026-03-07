package de.mrjulsen.paw.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.paw.block.abstractions.IRotatableBlock;
import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import de.mrjulsen.paw.data.BlockModificationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    
    boolean canModifyPos;
    Direction direction;
    BlockPos newPos;

    private BlockItem self() {
        return (BlockItem)(Object)this;
    }
    
    @Inject(method = "place", at = @At(value = "HEAD"), cancellable = true)
    private void onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPlaceContextExtension contextExtension = (BlockPlaceContextExtension)(Object)context;
        Level level = context.getLevel();
        BlockPos clickedBlockPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState clickedState = level.getBlockState(clickedBlockPos);
        Block thisBlock = self().getBlock();

        contextExtension.paw$setPlacedOnPos(clickedBlockPos);
        contextExtension.paw$setPlacedOnState(clickedState);
        canModifyPos = context.getClickedFace().getAxis().isHorizontal() && clickedState.getBlock() instanceof IRotatableBlock;
        if (canModifyPos) {
            IRotatableBlock supportRot = (IRotatableBlock)clickedState.getBlock();
            BlockModificationData value = supportRot.onPlaceOnRotatedBlock(context, clickedState, clickedBlockPos);
            if (thisBlock instanceof IRotatableBlock selfRot) {
                value = selfRot.onPlaceOnOtherRotatedBlock(value, context, clickedState, clickedBlockPos);
            }
            if (value != null) {
                newPos = value.newPos();
                direction = value.newDirection();    
                if (!level.getBlockState(newPos).canBeReplaced(context)) {
                    cir.setReturnValue(InteractionResult.FAIL);
                }
            } else {
                canModifyPos = false;
            }
        }
    }

    @ModifyVariable(method = "place", at = @At(value = "STORE"))
    private BlockPos modifyPlacementPos(BlockPos pos) {
        if (canModifyPos && direction != null) {
            pos = pos.relative(direction.getCounterClockWise());
        }
        return pos;
    }
        
    @Inject(method = "placeBlock", at = @At(value = "HEAD"), cancellable = true)
    private void onPlace(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (canModifyPos) {
            cir.setReturnValue(context.getLevel().setBlock(newPos, state, 11));
        }
    }
}
