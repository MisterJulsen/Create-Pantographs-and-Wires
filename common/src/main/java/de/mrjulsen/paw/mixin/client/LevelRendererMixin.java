package de.mrjulsen.paw.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import de.mrjulsen.paw.block.abstractions.IRotatableBlock;
import de.mrjulsen.paw.config.ModClientConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Render the custom wire rendertype and rotated block outline */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow
    private ClientLevel level;

    @Shadow
    private static void renderShape(PoseStack poseStack, VertexConsumer consumer, VoxelShape shape, double x, double y, double z, float red, float green, float blue, float alpha) {}
    
    @Inject(method = "renderHitOutline", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderHitOutline(PoseStack poseStack, VertexConsumer consumer, Entity entity, double camX, double camY, double camZ, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (state.getBlock() instanceof IRotatableBlock rot) {
            Vec2 pivot = rot.rotatedPivotPoint(state);
            poseStack.pushPose();
            Vec2 offset = rot.getOffset(state);
            poseStack.translate((double)pos.getX() - camX, (double)pos.getY() - camY, (double)pos.getZ() - camZ);
            poseStack.translate(pivot.x + offset.x, 0, pivot.y + offset.y);
            poseStack.pushPose();
            if (ModClientConfig.DEBUG_ORIGINAL_HITBOX.get()) {
                renderShape(poseStack, consumer, state.getShape(this.level, pos, CollisionContext.of(entity)), -pivot.x, 0, -pivot.y, 0.0F, 0.0F, 0.0F, 0.4F);
            } else {
                poseStack.mulPose(Axis.YP.rotationDegrees(rot.getRelativeYRotation(state)));
                renderShape(poseStack, consumer, rot.getBaseShape(state, this.level, pos, CollisionContext.of(entity)), -pivot.x, 0, -pivot.y, 0.0F, 0.0F, 0.0F, 0.4F);
            }
            poseStack.popPose();
            poseStack.popPose();

            ci.cancel();
        }
    }
}
