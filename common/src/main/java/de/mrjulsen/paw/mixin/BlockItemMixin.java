package de.mrjulsen.paw.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.paw.block.abstractions.IRotatableBlock;
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
    
    @Unique boolean paw$canModifyPos;
    @Unique Direction paw$direction;
    @Unique BlockPos paw$newPos;

    @Unique
    private BlockItem paw$self() {
        return (BlockItem)(Object)this;
    }
    
    @Inject(method = "place", at = @At(value = "HEAD"), cancellable = true)
    private void paw$onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos clickedBlockPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState clickedState = level.getBlockState(clickedBlockPos);
        Block thisBlock = paw$self().getBlock();

        paw$canModifyPos = context.getClickedFace().getAxis().isHorizontal() && clickedState.getBlock() instanceof IRotatableBlock;
        if (paw$canModifyPos) {
            IRotatableBlock supportRot = (IRotatableBlock)clickedState.getBlock();
            BlockModificationData value = supportRot.onPlaceOnRotatedBlock(context, clickedState, clickedBlockPos);
            if (thisBlock instanceof IRotatableBlock selfRot) {
                value = selfRot.onPlaceOnOtherRotatedBlock(value, context, clickedState, clickedBlockPos);
            }
            if (value != null) {
                paw$newPos = value.newPos();
                paw$direction = value.newDirection();
                if (!level.getBlockState(paw$newPos).canBeReplaced(context)) {
                    cir.setReturnValue(InteractionResult.FAIL);
                }
            } else {
                paw$canModifyPos = false;
            }
        }
    }

    @ModifyVariable(method = "place", at = @At(value = "STORE"))
    private BlockPos paw$modifyPlacementPos(BlockPos pos) {
        if (paw$canModifyPos && paw$direction != null) {
            pos = pos.relative(paw$direction.getCounterClockWise());
        }
        return pos;
    }
        
    @Inject(method = "placeBlock", at = @At(value = "HEAD"), cancellable = true)
    private void paw$onPlace(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (paw$canModifyPos) {
            cir.setReturnValue(context.getLevel().setBlock(paw$newPos, state, 11));
        }
    }
}
