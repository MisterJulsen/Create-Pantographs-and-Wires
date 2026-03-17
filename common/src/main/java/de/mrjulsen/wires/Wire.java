package de.mrjulsen.wires;

import java.util.Optional;

import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.wires.render.WireRenderData;

public record Wire(String name, Vector3d pos, @Nullable WirePoints collisionData, @Nullable WireRenderData renderData) {
    public Optional<WirePoints> getCollisionData() {
        return Optional.ofNullable(collisionData());
    }
    public Optional<WireRenderData> getRenderData() {
        return Optional.ofNullable(renderData());
    }
    public int renderingSegmentsCount() {
        return getRenderData().map(x -> x.count()).orElse(0);
    }
    public int collisionSegmentsCount() {
        return getCollisionData().map(x -> x.vertices().length).orElse(0);
    }
}
