package de.mrjulsen.wires.render;

import java.util.*;

import com.eliotlash.mclib.math.functions.limit.Min;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import de.mrjulsen.wires.decoration.WireDecorationData;
import de.mrjulsen.wires.decoration.WireDecorationRenderData;
import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;

public class WireSegmentRenderData {
    
    private final List<WireRenderPoint> points;
    private final List<WireDecorationRenderData> decorations;
    private double length;

    public WireSegmentRenderData() {
        this.points = new ArrayList<>();
        this.decorations = new ArrayList<>();
    }

    public double finish(TreeMap<Double, WireDecorationData> decorations, double lengthOffset) {
        this.decorations.clear();

        double oldLength = 0;
        this.length = 0;
        Vector3d a = new Vector3d(points.get(0).vertex(VertexCorner.CENTER));
        
        for (int i = 1; i < count(); i++) {
            Vector3d b = new Vector3d(points.get(i).vertex(VertexCorner.CENTER));
            Vector3d normal = new Vector3d(b).sub(a).normalize();
            oldLength = length;
            this.length += a.distance(b);
            
            Map.Entry<Double, WireDecorationData> entry = null;
            while (decorations != null && (entry = decorations.firstEntry()) != null) {
                double decoPos = entry.getKey() - lengthOffset;
                if (decoPos > oldLength && decoPos <= this.length) {
                    WireDecorationData deco = decorations.pollFirstEntry().getValue();
                    double localPos = decoPos - oldLength;
                    this.decorations.add(new WireDecorationRenderData(new Vector3d(a).add(new Vector3d(normal).mul(localPos)), normal, deco));
                } else {
                    break;
                }
            }
            a = b;
        }
        return this.length;
    }

    public void add(WireRenderPoint point) {
        this.points.add(point);
    }

    public WireRenderPoint getPoint(int index) {
        return points.get(index);
    }

    public int count() {
        return points.size();
    }

    public void render(BlockAndTintGetter level, SectionPos origin, VertexConsumer vertexConsumer, int color) {
        if (points.size() < 2) {
            return;
        }

        float u0 = WireRenderer.WIRE_TEXTURE.get().getU0();
        float v0 = WireRenderer.WIRE_TEXTURE.get().getV0();
        float u1 = WireRenderer.WIRE_TEXTURE.get().getU1();
        float v1 = WireRenderer.WIRE_TEXTURE.get().getV1();
        BlockPos originPos = origin.origin();

        WireRenderPoint lastVertices = points.get(0);
        for (int i = 1; i < points.size(); i++) {
            WireRenderPoint vertices = points.get(i);
            Vector3d center = vertices.vertex(VertexCorner.CENTER);
            int light = getLight(originPos.offset((int)center.x(), (int)center.y(), (int)center.z()), level);

            Vector3d vertex;
            vertex = lastVertices.vertex(VertexCorner.BOTTOM_LEFT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u0, v0).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            vertex = lastVertices.vertex(VertexCorner.TOP_RIGHT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u0, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            
            vertex = vertices.vertex(VertexCorner.TOP_RIGHT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u1, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            vertex = vertices.vertex(VertexCorner.BOTTOM_LEFT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u1, v0).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);

            // Opposite side
            vertex = lastVertices.vertex(VertexCorner.TOP_RIGHT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u0, v0).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            vertex = lastVertices.vertex(VertexCorner.BOTTOM_LEFT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u0, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            
            vertex = vertices.vertex(VertexCorner.BOTTOM_LEFT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u1, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            vertex = vertices.vertex(VertexCorner.TOP_RIGHT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u1, v0).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);


            
            vertex = lastVertices.vertex(VertexCorner.TOP_LEFT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u0, v0).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            vertex = lastVertices.vertex(VertexCorner.BOTTOM_RIGHT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u0, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            
            vertex = vertices.vertex(VertexCorner.BOTTOM_RIGHT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u1, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            vertex = vertices.vertex(VertexCorner.TOP_LEFT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u1, v0).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            
            // Opposite side
            vertex = lastVertices.vertex(VertexCorner.BOTTOM_RIGHT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u0, v0).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            vertex = lastVertices.vertex(VertexCorner.TOP_LEFT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u0, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            
            vertex = vertices.vertex(VertexCorner.TOP_LEFT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u1, v1).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);
            vertex = vertices.vertex(VertexCorner.BOTTOM_RIGHT);
            vertexConsumer.addVertex((float)vertex.x(), (float)vertex.y(), (float)vertex.z()).setColor(color).setUv(u1, v0).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(0, 0, 0);

            lastVertices = vertices;
        }

        PoseStack poseStack = new PoseStack();
        for (WireDecorationRenderData deco : decorations) {
            poseStack.pushPose();
            poseStack.translate(deco.worldPos().x(), deco.worldPos().y(), deco.worldPos().z());
            poseStack.pushPose();
            poseStack.mulPose(getYawPitchQuaternion(deco.direction()));
            BlockPos decoPos = originPos.offset((int)deco.worldPos().x(), (int)deco.worldPos().y(), (int)deco.worldPos().z());
            deco.data().getDecoration().getRenderer().render(poseStack, vertexConsumer, deco.worldPos(), deco.direction(), getLight(decoPos, level));
            poseStack.popPose();
            poseStack.popPose();
        }
    }

    private static int getLight(BlockPos pos, BlockAndTintGetter level) {
		try {
            return LevelRenderer.getLightColor(Objects.requireNonNullElse(Minecraft.getInstance().level, level), pos);
        } catch (Exception e) {
            return LightTexture.FULL_BLOCK;
        }
	}

    

	public static Quaternionf getYawPitchQuaternion(Vector3d direction) {
        Vector3d dir = new Vector3d(direction).normalize();
		float yaw = (float) Math.atan2(-dir.x, -dir.z);
		float pitch = (float) Math.asin(dir.y);
		return new Quaternionf().rotateY(yaw).rotateX(pitch);
	}
}
