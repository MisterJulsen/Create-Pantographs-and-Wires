package de.mrjulsen.wires.debug;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import de.mrjulsen.mcdragonlib.data.Pair;
import de.mrjulsen.wires.WireClientNetwork;
import de.mrjulsen.wires.WireCollision;
import de.mrjulsen.wires.WireCollision.WireBlockCollision;
import de.mrjulsen.wires.WireConnection;
import de.mrjulsen.wires.WireNetwork;
import de.mrjulsen.wires.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

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

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Entity entity = Minecraft.getInstance().gameRenderer.getMainCamera().getEntity();
        ChunkPos chunkPos = entity.chunkPosition();
  
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        MultiBufferSource.BufferSource mbs = Minecraft.getInstance().renderBuffers().bufferSource();
		VertexConsumer buffer = mbs.getBuffer(RenderType.lines());

        
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                ChunkPos relPos = new ChunkPos(chunkPos.x + a, chunkPos.z + b);
                for (WireConnection connection : WireNetwork.get(ClientUtils.level()).getConnectionsTroughChunk(relPos)) {
                    if (connection == null) continue;
                    for (WireBlockCollision c : connection.getCollisionData().getAllCollisions()) {
                        renderDebugLine(poseStack, buffer, new Vector3f((float)c.absA().x(), (float)c.absA().y() + 0.01f, (float)c.absA().z()), new Vector3f((float)c.absB().x(), (float)c.absB().y() + 0.01f, (float)c.absB().z()), 1f, 0f, 0f, 1f);
                    }
                }
                for (WireCollision collision : WireClientNetwork.get(ClientUtils.level()).getCollisionsTroughChunk(relPos)) {
                    for (WireBlockCollision c : collision.getAllCollisions()) {
                        renderDebugLine(poseStack, buffer, new Vector3f((float)c.absA().x(), (float)c.absA().y(), (float)c.absA().z()), new Vector3f((float)c.absB().x(), (float)c.absB().y(), (float)c.absB().z()), 0f, 0f, 1f, 1f);
                    }
                }
            }
        }

        while (!highlightedWires.isEmpty()) {
            Pair<Vector3f, Vector3f> p = highlightedWires.pollFirst();
            renderDebugLine(poseStack, buffer, new Vector3f(p.get()).sub(0, 0.01f, 0), new Vector3f(p.getSecond()).sub(0, 0.01f, 0), 0f, 1f, 0f, 1f);
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
}
