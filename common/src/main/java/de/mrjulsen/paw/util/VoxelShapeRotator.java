package de.mrjulsen.paw.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class VoxelShapeRotator {

    private static final double PIXEL         = 1.0 / 16.0;
    private static final double HALF_PIXEL    = PIXEL * 0.5;
    private static final double ANGLE_EPSILON = 0.01;

    private VoxelShapeRotator() {}

    public static VoxelShape rotateY(VoxelShape shape, float yawDeg) {
        return rotateY(shape, yawDeg, new Vec3(0.5, 0.5, 0.5));
    }

    /**
     * Rotates a VoxelShape around the Y axis.
     *
     * Convention matches AbstractRotatableBlock.getYRotation():
     *   NORTH=0°, WEST=90°, SOUTH=180°, EAST=270°  (clockwise from above)
     *
     * Internally we use standard math (CCW), so we negate the angle:
     *   x' =  cos(a)*dx + sin(a)*dz
     *   z' = -sin(a)*dx + cos(a)*dz
     * which equals CW rotation by 'a'.
     */
    public static VoxelShape rotateY(VoxelShape shape, float yawDeg, Vec3 pivot) {
        double angle = normalizeAngle(yawDeg);
        if (isNear(angle,   0.0)) return shape;
        if (isNear(angle,  90.0)) return transformExact(shape, pivot,  0,  1, -1,  0);
        if (isNear(angle, 180.0)) return transformExact(shape, pivot, -1,  0,  0, -1);
        if (isNear(angle, 270.0)) return transformExact(shape, pivot,  0, -1,  1,  0);
        return rasterizeRotated(shape, angle, pivot);
    }

    public static VoxelShape rotate(VoxelShape shape, float yawDeg, float pitchDeg, float rollDeg, Vec3 pivot) {
        return rotateY(shape, yawDeg, pivot);
    }

    public static VoxelShape rotate(VoxelShape shape, float yawDeg, float pitchDeg, float rollDeg) {
        return rotateY(shape, yawDeg);
    }

    /**
     * Exact 90°-step rotation via integer 2×2 XZ matrix — zero floating-point error.
     *
     * CW 90°:  (dx,dz) → ( dz, -dx)  →  a= 0, b= 1, c=-1, d= 0
     * CW 180°: (dx,dz) → (-dx, -dz)  →  a=-1, b= 0, c= 0, d=-1
     * CW 270°: (dx,dz) → (-dz,  dx)  →  a= 0, b=-1, c= 1, d= 0
     */
    private static VoxelShape transformExact(VoxelShape shape, Vec3 pivot, int a, int b, int c, int d) {
        List<AABB> result = new ArrayList<>();
        double px = pivot.x, pz = pivot.z;
        for (AABB box : shape.toAabbs()) {
            double dx0 = box.minX - px, dz0 = box.minZ - pz;
            double dx1 = box.maxX - px, dz1 = box.maxZ - pz;
            double nx0 = px + a * dx0 + b * dz0, nz0 = pz + c * dx0 + d * dz0;
            double nx1 = px + a * dx1 + b * dz1, nz1 = pz + c * dx1 + d * dz1;
            result.add(new AABB(
                    Math.min(nx0, nx1), box.minY, Math.min(nz0, nz1),
                    Math.max(nx0, nx1), box.maxY, Math.max(nz0, nz1)
            ));
        }
        return fromAabbs(result);
    }

    /**
     * Pixel-perfect rasterization for diagonal angles via inverse transform.
     *
     * For each pixel cell in the rotated bounding box, its XZ center is
     * back-projected into the original space. All original AABBs are tested —
     * a pixel column inherits the Y extents of every AABB whose XZ projection
     * contains the back-projected point.
     *
     * CW rotation by angle a:
     *   forward:  x' =  cos(a)*dx + sin(a)*dz,  z' = -sin(a)*dx + cos(a)*dz
     *   inverse:  x  =  cos(a)*dx'- sin(a)*dz', z  =  sin(a)*dx'+ cos(a)*dz'
     *   (transpose of orthogonal matrix)
     *
     * Worst case at 45° on a full block: ceil(sqrt(2)*16)² ≈ 529 XZ checks.
     */
    private static VoxelShape rasterizeRotated(VoxelShape shape, double angleDeg, Vec3 pivot) {
        double rad =  Math.toRadians(angleDeg);
        double cos =  Math.cos(rad);
        double sin =  Math.sin(rad);
        // CW forward:  x' = cos*dx + sin*dz,  z' = -sin*dx + cos*dz
        // CW inverse (transpose): x = cos*dx' - sin*dz', z = sin*dx' + cos*dz'
        double invCos =  cos;
        double invSin =  sin;

        double px = pivot.x, pz = pivot.z;
        List<AABB> boxes = shape.toAabbs();

        // Compute axis-aligned bounding box of all rotated corners
        double rMinX = Double.MAX_VALUE,  rMinZ = Double.MAX_VALUE;
        double rMaxX = -Double.MAX_VALUE, rMaxZ = -Double.MAX_VALUE;

        for (AABB box : boxes) {
            double dx0 = box.minX - px, dz0 = box.minZ - pz;
            double dx1 = box.maxX - px, dz1 = box.maxZ - pz;
            // CW: x' = cos*dx + sin*dz, z' = -sin*dx + cos*dz
            double[] rxs = {
                    px + cos*dx0 + sin*dz0, px + cos*dx1 + sin*dz0,
                    px + cos*dx1 + sin*dz1, px + cos*dx0 + sin*dz1
            };
            double[] rzs = {
                    pz - sin*dx0 + cos*dz0, pz - sin*dx1 + cos*dz0,
                    pz - sin*dx1 + cos*dz1, pz - sin*dx0 + cos*dz1
            };
            for (double x : rxs) { if (x < rMinX) rMinX = x; if (x > rMaxX) rMaxX = x; }
            for (double z : rzs) { if (z < rMinZ) rMinZ = z; if (z > rMaxZ) rMaxZ = z; }
        }

        int xStart = (int) Math.floor(rMinX / PIXEL);
        int xEnd   = (int) Math.ceil (rMaxX / PIXEL);
        int zStart = (int) Math.floor(rMinZ / PIXEL);
        int zEnd   = (int) Math.ceil (rMaxZ / PIXEL);

        List<AABB> accepted = new ArrayList<>();

        for (int xi = xStart; xi < xEnd; xi++) {
            double ddx = (xi * PIXEL + HALF_PIXEL) - px;
            for (int zi = zStart; zi < zEnd; zi++) {
                double ddz = (zi * PIXEL + HALF_PIXEL) - pz;
                // Back-project pixel center into original space (CW inverse)
                double ox = px + invCos * ddx - invSin * ddz;
                double oz = pz + invSin * ddx + invCos * ddz;

                double px0 = xi * PIXEL, px1 = px0 + PIXEL;
                double pz0 = zi * PIXEL, pz1 = pz0 + PIXEL;

                // Test ALL original AABBs — each match contributes its own Y slice
                for (AABB box : boxes) {
                    if (ox >= box.minX && ox <= box.maxX && oz >= box.minZ && oz <= box.maxZ) {
                        accepted.add(new AABB(px0, box.minY, pz0, px1, box.maxY, pz1));
                    }
                }
            }
        }

        return fromAabbs(accepted);
    }

    private static VoxelShape fromAabbs(List<AABB> aabbs) {
        if (aabbs.isEmpty()) return Shapes.empty();
        VoxelShape result = Shapes.empty();
        for (AABB a : aabbs) {
            result = Shapes.joinUnoptimized(result, Shapes.create(a), BooleanOp.OR);
        }
        return result.optimize();
    }

    public static Vec3 applyMatrix(Vec3 v, float[][] m, Vec3 pivot) {
        double dx = v.x - pivot.x, dy = v.y - pivot.y, dz = v.z - pivot.z;
        return new Vec3(
                pivot.x + m[0][0]*dx + m[0][1]*dy + m[0][2]*dz,
                pivot.y + m[1][0]*dx + m[1][1]*dy + m[1][2]*dz,
                pivot.z + m[2][0]*dx + m[2][1]*dy + m[2][2]*dz
        );
    }

    public static float[][] composeMatrix(float yawDeg, float pitchDeg, float rollDeg) {
        double yaw   = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double roll  = Math.toRadians(rollDeg);
        double cy = Math.cos(yaw),   sy = Math.sin(yaw);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double cr = Math.cos(roll),  sr = Math.sin(roll);
        double[][] ry = { { cy, 0, sy }, { 0, 1, 0 }, { -sy, 0, cy } };
        double[][] rx = { { 1, 0, 0 }, { 0, cp, -sp }, { 0, sp, cp } };
        double[][] rz = { { cr, -sr, 0 }, { sr, cr, 0 }, { 0, 0, 1 } };
        double[][] d  = multiplyD(ry, multiplyD(rx, rz));
        return new float[][] {
                { (float)d[0][0], (float)d[0][1], (float)d[0][2] },
                { (float)d[1][0], (float)d[1][1], (float)d[1][2] },
                { (float)d[2][0], (float)d[2][1], (float)d[2][2] }
        };
    }

    public static float[][] transposeMatrix(float[][] m) {
        return new float[][] {
                { m[0][0], m[1][0], m[2][0] },
                { m[0][1], m[1][1], m[2][1] },
                { m[0][2], m[1][2], m[2][2] }
        };
    }

    private static double[][] multiplyD(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                r[i][j] = a[i][0]*b[0][j] + a[i][1]*b[1][j] + a[i][2]*b[2][j];
        return r;
    }

    private static double normalizeAngle(float deg) {
        double a = deg % 360.0;
        return a < 0 ? a + 360.0 : a;
    }

    private static boolean isNear(double a, double target) {
        return Math.abs(a - target) < ANGLE_EPSILON;
    }
}