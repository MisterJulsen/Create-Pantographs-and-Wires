package de.mrjulsen.paw.util.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

import org.joml.Vector3d;
import org.joml.Vector3f;
import net.minecraft.world.phys.Vec3;

public class VoxelShapeRayTrace implements IRayTraceShape {
    private final VoxelShape shape;
    private final BlockPos pos;

    public VoxelShapeRayTrace(VoxelShape shape, BlockPos pos) {
        this.shape = shape;
        this.pos = pos;
    }

    @Override
    public Optional<Vector3d> intersects(Vector3d rayOrigin, Vector3d rayDirection) {
        Vec3 start = new Vec3(rayOrigin.x(), rayOrigin.y(), rayOrigin.z());
        Vec3 end = start.add(new Vec3(rayDirection.x(), rayDirection.y(), rayDirection.z()).scale(100)); // max ray length

        BlockHitResult result = shape.clip(start, end, pos);
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(new Vector3d(result.getLocation().x(), result.getLocation().y(), result.getLocation().z()));
    }
}

