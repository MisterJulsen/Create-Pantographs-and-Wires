package de.mrjulsen.paw.util.collision;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public class RaycastUtils {

    public static Optional<RaycastHitResult> rayTrace(Vector3f start, Vector3f end, Level level, float radius, ICollisionProvider collisionProvider) {
        Vector3f dir = new Vector3f(end).sub(start);
        float totalLength = dir.length();
        if (totalLength < 1e-6f) return Optional.empty();

        Vector3f normal = new Vector3f(dir).normalize();
        int iRadius = (int) Math.ceil(radius);

        int x = (int) Math.floor(start.x());
        int y = (int) Math.floor(start.y());
        int z = (int) Math.floor(start.z());

        int stepX = normal.x() >= 0 ? 1 : -1;
        int stepY = normal.y() >= 0 ? 1 : -1;
        int stepZ = normal.z() >= 0 ? 1 : -1;

        float tDeltaX = Math.abs(normal.x()) > 1e-9f ? Math.abs(1.0f / normal.x()) : Float.MAX_VALUE;
        float tDeltaY = Math.abs(normal.y()) > 1e-9f ? Math.abs(1.0f / normal.y()) : Float.MAX_VALUE;
        float tDeltaZ = Math.abs(normal.z()) > 1e-9f ? Math.abs(1.0f / normal.z()) : Float.MAX_VALUE;

        float tMaxX = tDeltaX * (stepX > 0 ? (float)(x + 1) - start.x() : start.x() - (float)x);
        float tMaxY = tDeltaY * (stepY > 0 ? (float)(y + 1) - start.y() : start.y() - (float)y);
        float tMaxZ = tDeltaZ * (stepZ > 0 ? (float)(z + 1) - start.z() : start.z() - (float)z);

        if (tMaxX < 1e-6f) tMaxX += tDeltaX;
        if (tMaxY < 1e-6f) tMaxY += tDeltaY;
        if (tMaxZ < 1e-6f) tMaxZ += tDeltaZ;

        Set<BlockPos> visited = new HashSet<>();
        float t = 0f;

        while (t <= totalLength) {
            for (int dx = -iRadius; dx <= iRadius; dx++) {
                for (int dy = -iRadius; dy <= iRadius; dy++) {
                    for (int dz = -iRadius; dz <= iRadius; dz++) {
                        BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
                        if (!visited.add(pos)) continue;

                        Optional<RaycastHitResult> hit =
                                collisionProvider.tryHit(level, pos, start, normal);
                        if (hit.isPresent()) {
                            return hit;
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