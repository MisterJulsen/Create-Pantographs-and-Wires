package de.mrjulsen.wires.render;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;

public class WireSegmentRenderDataBatch {

    private final UUID id;
    private final SectionPos section;
    private final Set<WireSegmentRenderData> segments = new HashSet<>();
    private boolean unloaded = false;

    public WireSegmentRenderDataBatch(UUID id, SectionPos section) {
        this.id = id;
        this.section = section;
    }

    public UUID getId() {
        return id;
    }

    public void addSegment(WireSegmentRenderData segment) {
        this.segments.add(segment);
    }

    public Set<WireSegmentRenderData> getSubWireSegments() {
        return segments;
    }

    public SectionPos getSection() {
        return section;
    }

    public boolean isUnloaded() {
        return unloaded;
    }

    public void setUnloaded(boolean unloaded) {
        this.unloaded = unloaded;
    }

    public void render(BlockAndTintGetter level, VertexConsumer vertexConsumer) {
        for (WireSegmentRenderData segment : segments) {
            segment.render(level, section, vertexConsumer, 0xFF252525);
        }
    }
}
