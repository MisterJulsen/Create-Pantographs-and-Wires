package de.mrjulsen.wires.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import de.mrjulsen.wires.decoration.WireDecorationData;
import de.mrjulsen.wires.decoration.WireDecorationRenderData;
import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class WireSegmentRenderData {
    
    private final List<WireRenderPoint> points;
    private final List<WireDecorationRenderData> decorations;
    private float length;

    public WireSegmentRenderData() {
        this.points = new ArrayList<>();
        this.decorations = new ArrayList<>();
    }

    public float finish(TreeMap<Float, WireDecorationData> decorations, float lengthOffset) {      
        this.decorations.clear();

        float oldLength = 0;
        this.length = 0;
        Vector3f a = new Vector3f(points.get(0).vertex(VertexCorner.CENTER));
        
        for (int i = 1; i < count(); i++) {
            Vector3f b = new Vector3f(points.get(i).vertex(VertexCorner.CENTER));
            Vector3f normal = new Vector3f(b).sub(a).normalize();
            oldLength = length;
            this.length += a.distance(b);
            
            Map.Entry<Float, WireDecorationData> entry = null;
            while (decorations != null && (entry = decorations.firstEntry()) != null) {
                float decoPos = entry.getKey() - lengthOffset;
                if (decoPos > oldLength && decoPos <= this.length) {
                    WireDecorationData deco = decorations.pollFirstEntry().getValue();
                    float localPos = decoPos - oldLength;
                    this.decorations.add(new WireDecorationRenderData(new Vector3f(a).add(new Vector3f(normal).mul(localPos)), normal, deco));
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

    public void render(VertexConsumer vertexConsumer, int color) {
        if (points.size() < 2) {
            return;
        }

        float u0 = WireRenderer.WIRE_TEXTURE.get().getU0();
        float v0 = WireRenderer.WIRE_TEXTURE.get().getV0();
        float u1 = WireRenderer.WIRE_TEXTURE.get().getU1();
        float v1 = WireRenderer.WIRE_TEXTURE.get().getV1();

        WireRenderPoint lastVertices = points.get(0);
        for (int i = 1; i < points.size(); i++) {
            WireRenderPoint vertices = points.get(i);
            
            Vector3f vertex;
            vertex = lastVertices.vertex(VertexCorner.BOTTOM_LEFT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u0, v0).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            vertex = lastVertices.vertex(VertexCorner.TOP_RIGHT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u0, v1).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            
            vertex = vertices.vertex(VertexCorner.TOP_RIGHT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u1, v1).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            vertex = vertices.vertex(VertexCorner.BOTTOM_LEFT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u1, v0).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();

            // Opposite side
            vertex = lastVertices.vertex(VertexCorner.TOP_RIGHT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u0, v0).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            vertex = lastVertices.vertex(VertexCorner.BOTTOM_LEFT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u0, v1).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            
            vertex = vertices.vertex(VertexCorner.BOTTOM_LEFT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u1, v1).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            vertex = vertices.vertex(VertexCorner.TOP_RIGHT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u1, v0).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();


            
            vertex = lastVertices.vertex(VertexCorner.TOP_LEFT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u0, v0).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            vertex = lastVertices.vertex(VertexCorner.BOTTOM_RIGHT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u0, v1).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            
            vertex = vertices.vertex(VertexCorner.BOTTOM_RIGHT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u1, v1).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            vertex = vertices.vertex(VertexCorner.TOP_LEFT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u1, v0).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            
            // Opposite side
            vertex = lastVertices.vertex(VertexCorner.BOTTOM_RIGHT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u0, v0).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            vertex = lastVertices.vertex(VertexCorner.TOP_LEFT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u0, v1).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            
            vertex = vertices.vertex(VertexCorner.TOP_LEFT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u1, v1).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();
            vertex = vertices.vertex(VertexCorner.BOTTOM_RIGHT);
            vertexConsumer.vertex(vertex.x(), vertex.y(), vertex.z()).color(color).uv(u1, v0).uv2(LightTexture.FULL_BRIGHT).overlayCoords(OverlayTexture.NO_OVERLAY).normal(0, 0, 0).endVertex();

            lastVertices = vertices;
        }

        PoseStack poseStack = new PoseStack();
        for (WireDecorationRenderData deco : decorations) {
            poseStack.pushPose();
            poseStack.translate(deco.worldPos().x(), deco.worldPos().y(), deco.worldPos().z());
            poseStack.pushPose();
            poseStack.mulPose(getYawPitchQuaternion(deco.direction()));
            deco.data().getDecoration().getRenderer().render(poseStack, vertexConsumer, deco.worldPos(), deco.direction());
            poseStack.popPose();
            poseStack.popPose();
        }
    }

    

	public static Quaternionf getYawPitchQuaternion(Vector3f direction) {
		Vector3f dir = new Vector3f(direction).normalize();
		float yaw = (float) Math.atan2(-dir.x, -dir.z);
		float pitch = (float) Math.asin(dir.y);
		return new Quaternionf().rotateY(yaw).rotateX(pitch);
	}
}
