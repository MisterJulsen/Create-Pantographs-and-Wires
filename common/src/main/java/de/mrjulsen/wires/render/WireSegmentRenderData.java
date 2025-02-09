package de.mrjulsen.wires.render;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class WireSegmentRenderData {
    
    private final List<WireRenderPoint> points;

    public WireSegmentRenderData() {
        this.points = new ArrayList<>();
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
    }
}
