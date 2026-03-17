package de.mrjulsen.wires.render;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.wires.decoration.WireDecorationData;
import de.mrjulsen.wires.render.WireRenderPoint.VertexCorner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public class WireRenderData {
    private final WireRenderPoint[] points;

    public WireRenderData(int size) {
        this.points = new WireRenderPoint[size];
    }

    public void setPoint(WireRenderPoint point, int index) {
        this.points[index] = point;
    }

    public WireRenderPoint getPoint(int index) {
        return points[index];
    }

    public int count() {
        return points.length;
    }

    public Map<SectionPos, WireSegmentRenderData> splitInChunkSections(TreeMap<Double, WireDecorationData> decorations) {
        Map<SectionPos, WireSegmentRenderData> result = new LinkedHashMap<>();
        Vector3d v = points[0].vertex(VertexCorner.CENTER);

        SectionPos lastSection = SectionPos.of(new BlockPos((int)v.x, (int)v.y, (int)v.z));
        WireRenderPoint lastVertices = points[0].offset(lastSection);
        result.computeIfAbsent(lastSection, x -> new WireSegmentRenderData()).add(lastVertices);

        for (int i = 1; i < points.length; i++) {
            v = points[i].vertex(VertexCorner.CENTER);
            SectionPos section = SectionPos.of(new BlockPos((int)v.x, (int)v.y, (int)v.z));
            WireRenderPoint vertices = points[i].offset(section);

            if (!lastSection.equals(section)) {
                result.computeIfAbsent(lastSection, x -> new WireSegmentRenderData()).add(points[i].offset(lastSection));
                result.computeIfAbsent(section, x -> new WireSegmentRenderData()).add(vertices);
            } else {
                result.computeIfAbsent(section, x -> new WireSegmentRenderData()).add(vertices);
            }

            lastVertices = vertices;
            lastSection = section;
        }

        float length = 0;
        for (WireSegmentRenderData segment : result.values()) {
            length += segment.finish(decorations, length);
        }

        return result;
    }



}
