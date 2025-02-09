package de.mrjulsen.wires.render;

import java.util.HashMap;
import java.util.Map;

import org.joml.Vector3f;

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

    public Map<SectionPos, WireSegmentRenderData> splitInChunkSections(SectionPos startSection) {
        Map<SectionPos, WireSegmentRenderData> result = new HashMap<>();
        Vector3f v = points[0].vertex(VertexCorner.CENTER);
        SectionPos lastRawSection = SectionPos.of(new BlockPos((int)v.x, (int)v.y, (int)v.z));
        WireRenderPoint lastVertices = points[0].offset(lastRawSection);
        SectionPos lastSection = lastRawSection.offset(startSection.getX(), startSection.getY(), startSection.getZ());
        result.computeIfAbsent(lastSection, x -> new WireSegmentRenderData()).add(lastVertices);

        for (int i = 1; i < points.length; i++) {
            v = points[i].vertex(VertexCorner.CENTER);
            SectionPos rawSection = SectionPos.of(new BlockPos((int)v.x, (int)v.y, (int)v.z));
            SectionPos section = rawSection.offset(startSection.getX(), startSection.getY(), startSection.getZ());
            WireRenderPoint vertices = points[i].offset(rawSection);
            if (lastSection.equals(section) || i < points.length - 1) {                
                result.computeIfAbsent(section, x -> new WireSegmentRenderData()).add(vertices);
            }
            if (!lastSection.equals(section)) {
                result.computeIfAbsent(lastSection, x -> new WireSegmentRenderData()).add(points[i].offset(lastRawSection));
            }

            lastVertices = vertices;
            lastSection = section;
            lastRawSection = rawSection;
        }

        return result;
    }


}
