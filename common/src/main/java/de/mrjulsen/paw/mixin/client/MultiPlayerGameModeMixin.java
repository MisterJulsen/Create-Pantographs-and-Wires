package de.mrjulsen.paw.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.paw.block.abstractions.IRotatableBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Fix block placement on rotated blocks */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    
    private LocalPlayer player;

    @Inject(method = "useItemOn", at = @At(value = "HEAD", ordinal = 0))
    private void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        this.player = player;
    }

    @ModifyVariable(method = "useItemOn", at = @At(value = "HEAD"))
    private BlockHitResult modifyHitResult(BlockHitResult hitResult) {
        if (hitResult.getDirection().getAxis() == Axis.Y) {
            return hitResult;
        }

        BlockState state = Minecraft.getInstance().level.getBlockState(hitResult.getBlockPos());
        if (state.getBlock() instanceof IRotatableBlock rot) {
            hitResult = rot.checkClickedFace(Minecraft.getInstance().level, player, hitResult);
            return hitResult;
        }
        return hitResult;
    }
}
