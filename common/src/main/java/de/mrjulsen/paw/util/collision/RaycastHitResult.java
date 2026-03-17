package de.mrjulsen.paw.util.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RaycastHitResult extends HitResult implements Comparable<RaycastHitResult> {
    private final BlockPos blockPos;
    private final double distance;
    private final Object hitData;

    public RaycastHitResult(Vec3 location, BlockPos blockPos, double distance, Object hitData) {
        super(location);
        this.blockPos = blockPos;
        this.distance = distance;
        this.hitData = hitData;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public double getDistance() {
        return distance;
    }

    public Object getHitData() {
        return hitData;
    }

    @Override
    public int compareTo(RaycastHitResult other) {
        return Double.compare(getDistance(), other.getDistance());
    }

    @Override
    public Type getType() {
        return Type.BLOCK;
    }
}
