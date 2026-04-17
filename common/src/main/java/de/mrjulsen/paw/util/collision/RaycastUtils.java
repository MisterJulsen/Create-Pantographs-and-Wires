package de.mrjulsen.paw.util.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class RaycastUtils {

    public static Optional<RaycastHitResult> rayTrace(Vector3d start, Vector3d end, Level level, float radius, ICollisionProvider collisionProvider) {

        Vector3d dir = new Vector3d(end).sub(start);
        double totalLength = dir.length();
        if (totalLength < 1e-6f) return Optional.empty();

        Vector3d normal = new Vector3d(dir).normalize();
        int iRadius = (int) Math.ceil(radius);

        Vec3 startVec = new Vec3(start.x(), start.y(), start.z());
        Vec3 endVec   = new Vec3(end.x(),   end.y(),   end.z());
        BlockHitResult blockHit = level.clip(new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));

        double maxFreeDistance = (blockHit.getType() == HitResult.Type.BLOCK)
                ? blockHit.getLocation().distanceTo(startVec)
                : totalLength;

        int x = (int) Math.floor(start.x());
        int y = (int) Math.floor(start.y());
        int z = (int) Math.floor(start.z());

        int stepX = normal.x() >= 0 ? 1 : -1;
        int stepY = normal.y() >= 0 ? 1 : -1;
        int stepZ = normal.z() >= 0 ? 1 : -1;

        double tDeltaX = Math.abs(normal.x()) > 1e-9f ? Math.abs(1.0f / normal.x()) : Float.MAX_VALUE;
        double tDeltaY = Math.abs(normal.y()) > 1e-9f ? Math.abs(1.0f / normal.y()) : Float.MAX_VALUE;
        double tDeltaZ = Math.abs(normal.z()) > 1e-9f ? Math.abs(1.0f / normal.z()) : Float.MAX_VALUE;

        double tMaxX = tDeltaX * (stepX > 0 ? (x + 1) - start.x() : start.x() - x);
        double tMaxY = tDeltaY * (stepY > 0 ? (y + 1) - start.y() : start.y() - y);
        double tMaxZ = tDeltaZ * (stepZ > 0 ? (z + 1) - start.z() : start.z() - z);

        if (tMaxX < 1e-6f) tMaxX += tDeltaX;
        if (tMaxY < 1e-6f) tMaxY += tDeltaY;
        if (tMaxZ < 1e-6f) tMaxZ += tDeltaZ;

        Set<BlockPos> visited = new HashSet<>();
        double t = 0d;

        while (t <= totalLength) {
            for (int dx = -iRadius; dx <= iRadius; dx++) {
                for (int dy = -iRadius; dy <= iRadius; dy++) {
                    for (int dz = -iRadius; dz <= iRadius; dz++) {
                        BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                        if (!visited.add(pos)) continue;

                        Optional<RaycastHitResult> hit = collisionProvider.tryHit(level, pos, start, normal);
                        if (hit.isPresent()) {
                            if (hit.get().getDistance() <= maxFreeDistance) {
                                return hit;
                            }
                        }
                    }
                }
            }

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                t = tMaxX; tMaxX += tDeltaX; x += stepX;
            } else if (tMaxY < tMaxZ) {
                t = tMaxY; tMaxY += tDeltaY; y += stepY;
            } else {
                t = tMaxZ; tMaxZ += tDeltaZ; z += stepZ;
            }
        }

        return Optional.empty();
    }
}