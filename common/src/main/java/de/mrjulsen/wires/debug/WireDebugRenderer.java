package de.mrjulsen.wires.debug;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.wires.graph.NewWireCollision;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

public class WireDebugRenderer {

    private static final Deque<Pair<Vector3f, Vector3f>> highlightedWires = new ConcurrentLinkedDeque<>();

    public static boolean enabled() {
        return Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
    }
    
    public static void renderWireCollisions(PoseStack poseStack) {
        if (!enabled()) {
            if (!highlightedWires.isEmpty()) highlightedWires.clear();
            return;
        }

        Vector3f cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f();
        Entity entity = Minecraft.getInstance().gameRenderer.getMainCamera().getEntity();
        ChunkPos chunkPos = entity.chunkPosition();
  
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        MultiBufferSource.BufferSource mbs = Minecraft.getInstance().renderBuffers().bufferSource();

        for (WireGraphClient graph : WireGraphManager.getAllClient(Minecraft.getInstance().level)) {           
            for (WireNode node : graph.getNodes()) {
                if (node.getPos().distance(cameraPos) > 16) continue;
                renderNameTag(poseStack, mbs, LightTexture.FULL_BRIGHT, node.getPos(), 0.2f, List.of(
                    TextUtils.text("ID: " + node.getId().toString()),
                    TextUtils.text("Graph: " + node.getGraph().getId().id()),
                    TextUtils.text("Type: " + node.getData().getRegistryType().id()),
                    TextUtils.text("Pos: " + node.getPos()),
                    TextUtils.text("Connections: " + node.getConnections().size())
                ));
            }
            for (WireEdge edge : graph.getEdges()) {
                if (edge.getCenterPos().distance(cameraPos) > 16) continue;
                renderNameTag(poseStack, mbs, LightTexture.FULL_BRIGHT, graph.getNode(edge.getNodeAId()).getPos().add(graph.getNode(edge.getNodeBId()).getPos()).div(2), 0.2f, List.of(
                    TextUtils.text("ID: " + edge.getId()),
                    TextUtils.text("Graph: " + edge.getGraph().getId().id()),
                    TextUtils.text("Node Type[A]: " + edge.getWireConnectionData().connectorA().getRegistryType().id()).withStyle(ChatFormatting.LIGHT_PURPLE),
                    TextUtils.text("Node Type[B]: " + edge.getWireConnectionData().connectorB().getRegistryType().id()).withStyle(ChatFormatting.AQUA),
                    TextUtils.text("Wire Type: " + edge.getType().getRegistryId())
                ));
            }

            VertexConsumer buffer = mbs.getBuffer(RenderType.lines());        

            for (WireNode node : graph.getNodes()) {
                renderDebugLine(poseStack, buffer, new Vector3f(node.getPos()).sub(0.25f, 0, 0), new Vector3f(node.getPos()).add(0.25f, 0, 0), 1, 1, 0, 1);
                renderDebugLine(poseStack, buffer, new Vector3f(node.getPos()).sub(0, 0.25f, 0), new Vector3f(node.getPos()).add(0, 0.25f, 0), 1, 1, 0, 1);
                renderDebugLine(poseStack, buffer, new Vector3f(node.getPos()).sub(0, 0, 0.25f), new Vector3f(node.getPos()).add(0, 0, 0.25f), 1, 1, 0, 1);
            }
            for (WireEdge edge : graph.getEdges()) {
                renderDebugLineGradient(poseStack, buffer, graph.getNode(edge.getNodeAId()).getPos(), graph.getNode(edge.getNodeBId()).getPos(), 1, 0, 1, 1, 0, 1, 1, 1);
            }

            for (int a = -2; a <= 2; a++) {
                for (int b = -2; b <= 2; b++) {
                    ChunkPos relPos = new ChunkPos(chunkPos.x + a, chunkPos.z + b);
                    for (NewWireCollision collision : graph.getCollisionsInChunk(relPos)) {
                        if (collision == null) continue;
                        for (NewWireCollision.WireBlockCollision c : collision.getAllCollisions()) {
                            renderDebugLine(poseStack, buffer, new Vector3f((float)c.getAbsoluteInVector().x(), (float)c.getAbsoluteInVector().y() + 0.01f, (float)c.getAbsoluteInVector().z()), new Vector3f((float)c.getAbsoluteOutVector().x(), (float)c.getAbsoluteOutVector().y() + 0.01f, (float)c.getAbsoluteOutVector().z()), 1f, 0f, 0f, 1f);
                        }
                    }
                }
            }

            while (!highlightedWires.isEmpty()) {
                Pair<Vector3f, Vector3f> p = highlightedWires.pollFirst();
                renderDebugLine(poseStack, buffer, new Vector3f(p.get()).sub(0, 0.01f, 0), new Vector3f(p.getSecond()).sub(0, 0.01f, 0), 0f, 1f, 0f, 1f);
            }         
        }

        poseStack.popPose();
    }

    public static void highlightWire(Vector3f a, Vector3f b) {
        if (!enabled()) return;
        highlightedWires.addLast(Pair.of(a, b));
    }

    public static void renderDebugLine(PoseStack poseStack, VertexConsumer consumer, Vector3f from, Vector3f to, float r, float g, float b, float a) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();

        float dx = to.x() - from.x();
        float dy = to.y() - from.y();
        float dz = to.z() - from.z();
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }

        consumer.vertex(matrix4f, (float) from.x(), (float) from.y(), (float) from.z()).color(r, g, b, a).normal(matrix3f, dx, dy, dz).endVertex();
        consumer.vertex(matrix4f, (float) to.x(), (float) to.y(), (float) to.z()).color(r, g, b, a).normal(matrix3f, dx, dy, dz).endVertex();
    }
    
    public static void renderDebugLineGradient(PoseStack poseStack, VertexConsumer consumer, Vector3f from, Vector3f to, float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();

        float dx = to.x() - from.x();
        float dy = to.y() - from.y();
        float dz = to.z() - from.z();
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }

        consumer.vertex(matrix4f, (float) from.x(), (float) from.y(), (float) from.z()).color(r1, g1, b1, a1).normal(matrix3f, dx, dy, dz).endVertex();
        consumer.vertex(matrix4f, (float) to.x(), (float) to.y(), (float) to.z()).color(r2, g2, b2, a2).normal(matrix3f, dx, dy, dz).endVertex();
    }

    protected static void renderNameTag(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Vector3f pos, float yOffset, List<Component> text) {
        if (Minecraft.getInstance().getCameraEntity().position().toVector3f().distance(pos) > 64) {
            return;
        }
        final float lineHeight = Minecraft.getInstance().font.lineHeight * 1.5f;
        poseStack.pushPose();
        poseStack.translate(pos.x(), pos.y() + yOffset, pos.z());
        poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        poseStack.scale(-0.0125F, -0.0125F, 0.0125F);
        Matrix4f matrix4f = poseStack.last().pose();

        for (int k = 0; k < text.size(); k++) {
            float y = lineHeight * k;
            Component txt = text.get(k);

            float opacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            int backgroundColor = (int)(opacity * 255.0F) << 24;
            Font font = Minecraft.getInstance().font;
            float x = (float)(-font.width(txt) / 2);

            font.drawInBatch(txt, x, -(lineHeight * text.size()) + y, 553648127, false, matrix4f, buffer, Font.DisplayMode.SEE_THROUGH, backgroundColor, packedLight);
            font.drawInBatch(txt, x, -(lineHeight * text.size()) + y, -1, false, matrix4f, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        }

        poseStack.popPose();
    }
}
