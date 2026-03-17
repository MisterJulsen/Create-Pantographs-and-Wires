package de.mrjulsen.wires.render;

import java.util.Map;

import org.joml.Vector3d;
import org.joml.Vector3f;

import net.minecraft.core.SectionPos;

public class WireRenderPoint {
    private final Map<VertexCorner, Vector3d> vertices;

    public WireRenderPoint(Map<VertexCorner, Vector3d> data) {
        this.vertices = data;
    }

    public Vector3d vertex(VertexCorner corner) {
        return vertices.get(corner);
    }

	public static enum VertexCorner { CENTER, TOP_RIGHT, BOTTOM_RIGHT, TOP_LEFT, BOTTOM_LEFT; }

    public WireRenderPoint offset(SectionPos rawSection) {
        Vector3d sub = new Vector3d(rawSection.x() * SectionPos.SECTION_SIZE, rawSection.y() * SectionPos.SECTION_SIZE, rawSection.z() * SectionPos.SECTION_SIZE);
        return new WireRenderPoint(Map.of(
            VertexCorner.CENTER, new Vector3d(vertex(VertexCorner.CENTER)).sub(sub),
            VertexCorner.TOP_RIGHT, new Vector3d(vertex(VertexCorner.TOP_RIGHT)).sub(sub),
            VertexCorner.BOTTOM_RIGHT, new Vector3d(vertex(VertexCorner.BOTTOM_RIGHT)).sub(sub),
            VertexCorner.TOP_LEFT, new Vector3d(vertex(VertexCorner.TOP_LEFT)).sub(sub),
            VertexCorner.BOTTOM_LEFT, new Vector3d(vertex(VertexCorner.BOTTOM_LEFT)).sub(sub)
        ));
    }

}
