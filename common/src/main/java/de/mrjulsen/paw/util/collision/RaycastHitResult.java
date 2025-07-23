package de.mrjulsen.paw.util.collision;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;

public record RaycastHitResult(Vector3f hitPosition, BlockPos blockPos, float distance, Object metadata) implements Comparable<RaycastHitResult> {

    @Override
    public int compareTo(RaycastHitResult other) {
        return Float.compare(distance(), other.distance());
    }
}
