package de.mrjulsen.paw.blockentity.client;

import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;

import de.mrjulsen.mcdragonlib.client.ber.BERGraphics;
import de.mrjulsen.mcdragonlib.client.ber.SafeBlockEntityRenderer;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity.CantileverShapeData;
import de.mrjulsen.paw.util.ClientUtils;
import de.mrjulsen.wires.debug.WireDebugRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;

public class CantileverBlockRenderer extends SafeBlockEntityRenderer<CantileverBlockEntity> {
        
    public CantileverBlockRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(BERGraphics<CantileverBlockEntity> graphics, float partialTicks) {
        if (!WireDebugRenderer.enabled()) {
            return;
        }

        graphics.poseStack().pushPose();
		VertexConsumer consumer = graphics.multiBufferSource().getBuffer(RenderType.lines());
        Vector3f blockPos = new Vector3f(graphics.blockEntity().getBlockPos().getX(), graphics.blockEntity().getBlockPos().getY(), graphics.blockEntity().getBlockPos().getZ());
        for (int i = 0; i < graphics.blockEntity().getSubCantileverSettings().size(); i++) {
            CantileverShapeData shape = graphics.blockEntity().getCantileverInteractionShape(i);
            RenderSystem.lineWidth(5);
            ClientUtils.renderDebugLine(graphics.poseStack(), consumer, new Vector3f(shape.stayTubeRoot()).sub(blockPos), new Vector3f(shape.front()).sub(blockPos), 0, 1, 0, 1);
            ClientUtils.renderDebugLine(graphics.poseStack(), consumer, new Vector3f(shape.bracketTubeRoot()).sub(blockPos), new Vector3f(shape.front()).sub(blockPos), 0, 1, 0, 1);
        }
        graphics.poseStack().popPose();
    }
}
