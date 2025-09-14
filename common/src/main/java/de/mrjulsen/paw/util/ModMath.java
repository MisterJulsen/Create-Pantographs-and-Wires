package de.mrjulsen.paw.util;

import org.joml.Vector3f;
import org.joml.Vector3i;

import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ModMath {

	public static final Vector3f CENTER_OF_ORIGIN = new Vector3f(0.5f, 0.5f, 0.5f);

    public static Vec2 rotateY(Vec2 vec, double deg) {
		if (deg == 0)
			return vec;
		if (vec == Vec2.ZERO)
			return vec;

		float angle = (float) (deg / 180f * Math.PI);
		double sin = Mth.sin(angle);
		double cos = Mth.cos(angle);
		double x = vec.x;
		double y = vec.y;
        return new Vec2((float)(x * cos + y * sin), (float)(y * cos - x * sin));
	}
    
	public static VoxelShape moveShape(VoxelShape shape, Vec3 vec) {
        AABB[] aabbs = shape.toAabbs().toArray(AABB[]::new);
        VoxelShape[] shapes = new VoxelShape[aabbs.length];
        for (int i = 0; i < aabbs.length; i++) {
            shapes[i] = Shapes.create(moveAABB(aabbs[i], vec));
        }
        return Shapes.or(Shapes.empty(), shapes);
    }

    public static AABB moveAABB(AABB aabb, Vec3 vec) {
        return new AABB(aabb.minX + vec.x, aabb.minY + vec.y, aabb.minZ + vec.z, aabb.maxX + vec.x, aabb.maxY + vec.y, aabb.maxZ + vec.z);
    }

	public static VoxelShape rotateShape(VoxelShape shape, Axis axis, int degrees) {
        AABB[] aabbs = shape.toAabbs().toArray(AABB[]::new);
        VoxelShape[] shapes = new VoxelShape[aabbs.length];
        for (int i = 0; i < aabbs.length; i++) {
            shapes[i] = Shapes.create(rotateAABB(aabbs[i], axis, degrees));
        }
        return Shapes.or(Shapes.empty(), shapes);
    }

    public static AABB rotateAABB(AABB aabb, Axis axis, int degrees) {
        int normalizedDegrees = ((degrees % 360) + 360) % 360;
        if (normalizedDegrees == 0) return aabb;

        double minX = aabb.minX;
        double minY = aabb.minY;
        double minZ = aabb.minZ;
        double maxX = aabb.maxX;
        double maxY = aabb.maxY;
        double maxZ = aabb.maxZ;
        
        switch (axis) {
            case X:
                switch (normalizedDegrees) {
                    case 90:
                        return new AABB(minX, -maxZ, minY, maxX, -minZ, maxY);
                    case 180:
                        return new AABB(minX, -maxY, -maxZ, maxX, -minY, -minZ);
                    case 270:
                        return new AABB(minX, minZ, -maxY, maxX, maxZ, -minY);
                }
                break;
            case Y:
                switch (normalizedDegrees) {
                    case 90:
                        return new AABB(1f-maxZ, minY, minX, 1f-minZ, maxY, maxX);
                    case 180:
                        return new AABB(1f-maxX, minY, 1f-maxZ, 1f-minX, maxY, 1f-minZ);
                    case 270:
                        return new AABB(minZ, minY, 1f-maxX, maxZ, maxY, 1f-minX);
                }
                break;
            case Z:
                switch (normalizedDegrees) {
                    case 90:
                        return new AABB(-maxY, minX, minZ, -minY, maxX, maxZ);
                    case 180:
                        return new AABB(-maxX, -maxY, minZ, -minX, -minY, maxZ);
                    case 270:
                        return new AABB(minY, -maxX, minZ, maxY, -minX, maxZ);
                }
                break;
            default:
                throw new IllegalArgumentException("Axis must be 'x', 'y', or 'z'");
        }
        throw new IllegalArgumentException("Degrees must be 0, 90, 180, or 270");
    }


	public static VoxelShape scaleShape(VoxelShape shape, Axis axis, double factor, double pivot) {
        AABB[] aabbs = shape.toAabbs().toArray(AABB[]::new);
        VoxelShape[] shapes = new VoxelShape[aabbs.length];
        for (int i = 0; i < aabbs.length; i++) {
            shapes[i] = Shapes.create(scaleAABB(aabbs[i], axis, factor, pivot));
        }
        return Shapes.or(Shapes.empty(), shapes);
    }

    /**
     * Scales the AABB along an axis by a factor with an optional pivot.
     *
     * @param axis The axis along which scaling occurs.
     * @param factor The scaling factor (1.0 = no change, >1.0 = increase in size, <1.0 = decrease in size).
     * @param pivot The pivot point (default = 0.5 = center).
     * @return The scaled AABB.
     */
    public static AABB scaleAABB(AABB aabb, Axis axis, double factor, double pivot) {
        double min, max;
        switch (axis) {
            case X:
                min = aabb.minX;
                max = aabb.maxX;
                break;            
            case Y:
                min = aabb.minY;
                max = aabb.maxY;
                break;
            case Z:
                min = aabb.minZ;
                max = aabb.maxZ;
                break;
            default:
                throw new IllegalArgumentException("Axis must be 'x', 'y', or 'z'");
        }

        double minDiff = 0.5d - min;
        double maxDiff = max - 0.5d;
        double newMin = 0.5d - (factor * minDiff);
        double newMax = 0.5d + (factor * maxDiff);

        return switch (axis) {
            case X -> new AABB(newMin, aabb.minY, aabb.minZ, newMax, aabb.maxY, aabb.maxZ);
            case Y -> new AABB(aabb.minX, newMin, aabb.minZ, aabb.maxX, newMax, aabb.maxZ);
            case Z -> new AABB(aabb.minX, aabb.minY, newMin, aabb.maxX, aabb.maxY, newMax);
            default -> aabb;
        };
    }


	public static VoxelShape scaleShapeOneSide(VoxelShape shape, Axis axis, double factor, AxisDirection direction) {
        AABB[] aabbs = shape.toAabbs().toArray(AABB[]::new);
        VoxelShape[] shapes = new VoxelShape[aabbs.length];
        for (int i = 0; i < aabbs.length; i++) {
            shapes[i] = Shapes.create(scaleAABBOneSide(aabbs[i], axis, factor, direction));
        }
        return Shapes.or(Shapes.empty(), shapes);
    }

     /**
     * Scales the AABB along an axis, keeping one side fixed.
     *
     * @param axis The axis to scale along.
     * @param factor The scaling factor (>1.0 = increase, <1.0 = decrease).
     * @param direction The direction that remains fixed.
     * @return The rescaled AABB.
     */
    public static AABB scaleAABBOneSide(AABB aabb, Axis axis, double factor, AxisDirection direction) {
        double min, max;
        switch (axis) {
            case X:
                min = aabb.minX;
                max = aabb.maxX;
                break;            
            case Y:
                min = aabb.minY;
                max = aabb.maxY;
                break;
            case Z:
                min = aabb.minZ;
                max = aabb.maxZ;
                break;
            default:
                throw new IllegalArgumentException("Axis must be 'x', 'y', or 'z'");
        }

        double minDiff = 0.5d - min;
        double maxDiff = max - 0.5d;
        double newMin = direction == AxisDirection.NEGATIVE ? 0.5d - (factor * minDiff) : min;
        double newMax = direction == AxisDirection.POSITIVE ? 0.5d + (factor * maxDiff) : max;

        return switch (axis) {
            case X -> new AABB(newMin, aabb.minY, aabb.minZ, newMax, aabb.maxY, aabb.maxZ);
            case Y -> new AABB(aabb.minX, newMin, aabb.minZ, aabb.maxX, newMax, aabb.maxZ);
            case Z -> new AABB(aabb.minX, aabb.minY, newMin, aabb.maxX, aabb.maxY, newMax);
            default -> aabb;
        };
    }

    public static int checkPointPosition(Vec2 pointA, Vec2 pointB, Vec2 pointP) {
        return (int)Math.signum((pointB.x - pointA.x) * (pointP.y - pointA.y) - (pointB.y - pointA.y) * (pointP.x - pointA.x));
    }
    
    public static Vector3f rotate(Vector3f vec, Vector3f rotationVec) {
		return rotate(vec, rotationVec.x, rotationVec.y, rotationVec.z);
	}

	public static Vector3f rotate(Vector3f vec, double xRot, double yRot, double zRot) {
		return rotate(rotate(rotate(vec, xRot, Axis.X), yRot, Axis.Y), zRot, Axis.Z);
	}

	public static Vector3f rotateCentered(Vector3f vec, double deg, Axis axis) {
		Vector3f shift = getCenterOf(new Vector3i());
		return rotate(new Vector3f(vec).sub(shift), deg, axis).add(shift);
	}

	public static Vector3f rotate(Vector3f vec, double deg, Axis axis) {
		if (deg == 0)
			return vec;

		float angle = (float) (deg / 180f * Math.PI);
		float sin = Mth.sin(angle);
		float cos = Mth.cos(angle);
		float x = vec.x;
		float y = vec.y;
		float z = vec.z;

		if (axis == Axis.X)
			return new Vector3f(x, y * cos - z * sin, z * cos + y * sin);
		if (axis == Axis.Y)
			return new Vector3f(x * cos + z * sin, y, z * cos - x * sin);
		if (axis == Axis.Z)
			return new Vector3f(x * cos - y * sin, y * cos + x * sin, z);
		return vec;
	}

	public static Vector3f getCenterOf(Vector3i pos) {
		if (pos.equals(new Vector3i()))
			return CENTER_OF_ORIGIN;
		return new Vector3f(pos).add(.5f, .5f, .5f);
	}


    public static Vector3f centerOf(Vector3f... points) {
        if (points == null || points.length == 0) {
            return null;
        }

        Vector3f sum = new Vector3f(0, 0, 0);

        for (Vector3f v : points) {
            sum.add(v);
        }

        sum.div(points.length);

        return sum;
    }

    public static double snap(double x, double a) {
        if (a <= 0) throw new IllegalArgumentException("a must be > 0");
        return Math.floor(x / a) * a;
    }

    public static double snapNearest(double x, double a) {
        if (a <= 0) throw new IllegalArgumentException("a must be > 0");
        return Math.round(x / a) * a;
    }
}
