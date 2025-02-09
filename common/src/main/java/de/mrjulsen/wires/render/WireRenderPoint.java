package de.mrjulsen.wires.render;

import java.util.Map;

import org.joml.Vector3f;

import net.minecraft.core.SectionPos;

public class WireRenderPoint {
    private final Map<VertexCorner, Vector3f> vertices;

    public WireRenderPoint(Map<VertexCorner, Vector3f> data) {
        this.vertices = data;
    }

    public Vector3f vertex(VertexCorner corner) {
        return vertices.get(corner);
    }

	public static enum VertexCorner { CENTER, TOP_RIGHT, BOTTOM_RIGHT, TOP_LEFT, BOTTOM_LEFT; }

    public WireRenderPoint offset(SectionPos rawSection) {
        Vector3f sub = new Vector3f(rawSection.x() * SectionPos.SECTION_SIZE, rawSection.y() * SectionPos.SECTION_SIZE, rawSection.z() * SectionPos.SECTION_SIZE);
        return new WireRenderPoint(Map.of(
            VertexCorner.CENTER, new Vector3f(vertex(VertexCorner.CENTER)).sub(sub),
            VertexCorner.TOP_RIGHT, new Vector3f(vertex(VertexCorner.TOP_RIGHT)).sub(sub),
            VertexCorner.BOTTOM_RIGHT, new Vector3f(vertex(VertexCorner.BOTTOM_RIGHT)).sub(sub),
            VertexCorner.TOP_LEFT, new Vector3f(vertex(VertexCorner.TOP_LEFT)).sub(sub),
            VertexCorner.BOTTOM_LEFT, new Vector3f(vertex(VertexCorner.BOTTOM_LEFT)).sub(sub)
        ));
    }

}
