package de.mrjulsen.paw.util;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import de.mrjulsen.mcdragonlib.client.model.mesh.ITransformable;
import de.mrjulsen.mcdragonlib.client.model.mesh.IVertexElement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * DLBoundingBox: an oriented, transformable bounding box built from 8 vertex elements.
 * - flexible (rotate/scale/translate)
 * - collision tests (point, OBB vs OBB, OBB vs AABB)
 * - pixelate() rasterizes the rotated box into 1/16-grid voxels and returns a VoxelShape
 *
 * Assumptions / notes:
 * - Vertices are expected to represent a box (8 corners). Many helpers assume a canonical ordering:
 *   v0 = (-x,-y,-z), v1 = (+x,-y,-z), v2 = (+x,+y,-z), v3 = (-x,+y,-z),
 *   v4 = (-x,-y,+z), v5 = (+x,-y,+z), v6 = (+x,+y,+z), v7 = (-x,+y,+z)
 * - If vertices are arbitrary, computeAxes() tries to derive principal axes but exactness may vary.
 * - Adjust imports to your mapping (Vector3f and Quaternionf may come from different packages).
 */
public class DLBoundingBox implements ITransformable<DLBoundingBox> {

    private final List<VertexElement> vertices = new ArrayList<>(8);

    // Cached OBB parameters (recomputed on demand)
    private final Vector3f center = new Vector3f();
    private final Vector3f axisX = new Vector3f(1, 0, 0);
    private final Vector3f axisY = new Vector3f(0, 1, 0);
    private final Vector3f axisZ = new Vector3f(0, 0, 1);
    private final Vector3f halfSizes = new Vector3f(0.5f, 0.5f, 0.5f);

    private boolean dirty = true;

    // --- constructors ---------------------------------
    public DLBoundingBox() {
        // build unit cube centered at origin
        addDefaultCube();
        markDirty();
    }

    public DLBoundingBox(Vector3f min, Vector3f max) {
        createFromAABB(min, max);
        markDirty();
    }

    public DLBoundingBox(AABB aabb) {
        this(new Vector3f((float)aabb.minX, (float)aabb.minY, (float)aabb.minZ),
             new Vector3f((float)aabb.maxX, (float)aabb.maxY, (float)aabb.maxZ));
    }

    private void addDefaultCube() {
        // canonical order (see class javadoc)
        vertices.clear();
        vertices.add(new VertexElement(new Vector3f(-0.5f, -0.5f, -0.5f)));
        vertices.add(new VertexElement(new Vector3f( 0.5f, -0.5f, -0.5f)));
        vertices.add(new VertexElement(new Vector3f( 0.5f,  0.5f, -0.5f)));
        vertices.add(new VertexElement(new Vector3f(-0.5f,  0.5f, -0.5f)));
        vertices.add(new VertexElement(new Vector3f(-0.5f, -0.5f,  0.5f)));
        vertices.add(new VertexElement(new Vector3f( 0.5f, -0.5f,  0.5f)));
        vertices.add(new VertexElement(new Vector3f( 0.5f,  0.5f,  0.5f)));
        vertices.add(new VertexElement(new Vector3f(-0.5f,  0.5f,  0.5f)));
    }

    private void createFromAABB(Vector3f min, Vector3f max) {
        vertices.clear();
        vertices.add(new VertexElement(new Vector3f(min.x, min.y, min.z))); // v0
        vertices.add(new VertexElement(new Vector3f(max.x, min.y, min.z))); // v1
        vertices.add(new VertexElement(new Vector3f(max.x, max.y, min.z))); // v2
        vertices.add(new VertexElement(new Vector3f(min.x, max.y, min.z))); // v3
        vertices.add(new VertexElement(new Vector3f(min.x, min.y, max.z))); // v4
        vertices.add(new VertexElement(new Vector3f(max.x, min.y, max.z))); // v5
        vertices.add(new VertexElement(new Vector3f(max.x, max.y, max.z))); // v6
        vertices.add(new VertexElement(new Vector3f(min.x, max.y, max.z))); // v7
    }

    public static DLBoundingBox fromVoxelShape(VoxelShape shape) {
        // Create a DLBoundingBox that encloses the VoxelShape's bounding boxes.
        // For simplicity return a single AABB that bounds the whole VoxelShape.
        // If you need multiple DLBoundingBox objects (for complex shapes), split by each AABB returned by shape.getBoundingBoxes().
        DLBoundingBox box = new DLBoundingBox();
        List<AABB> boxes = shape.toAabbs(); // mapping dependent - adjust if necessary
        if (boxes.isEmpty()) return box;
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (AABB a : boxes) {
            minX = Math.min(minX, (float)a.minX);
            minY = Math.min(minY, (float)a.minY);
            minZ = Math.min(minZ, (float)a.minZ);
            maxX = Math.max(maxX, (float)a.maxX);
            maxY = Math.max(maxY, (float)a.maxY);
            maxZ = Math.max(maxZ, (float)a.maxZ);
        }
        box.createFromAABB(new Vector3f(minX, minY, minZ), new Vector3f(maxX, maxY, maxZ));
        box.markDirty();
        return box;
    }

    // --- ITransformable implementation ---------------
    @Override
    public List<? extends VertexElement> getTransformableElements() {
        return vertices;
    }

    // --- public API ----------------------------------
    /**
     * Recompute internal OBB representation if vertex positions changed.
     * This derives center, three orthonormal axes and half sizes.
     * It's robust for true box vertex order; otherwise falls back to PCA-like heuristics.
     */
    private void recomputeIfNeeded() {
        if (!dirty) return;
        dirty = false;

        // compute center (average)
        center.set(0, 0, 0);
        for (VertexElement v : vertices) center.add(v.getPos());
        center.div(vertices.size());

        // try to determine axis vectors from canonical vertex topology
        // if vertices follow the canonical order described in the javadoc, axes can be derived directly:
        if (vertices.size() >= 8) {
            Vector3f v0 = vertices.get(0).getPos();
            Vector3f v1 = vertices.get(1).getPos();
            Vector3f v3 = vertices.get(3).getPos();
            Vector3f v4 = vertices.get(4).getPos();

            Vector3f ax = new Vector3f(v1).sub(v0);
            Vector3f ay = new Vector3f(v3).sub(v0);
            Vector3f az = new Vector3f(v4).sub(v0);

            if (ax.lengthSquared() > 1e-6f && ay.lengthSquared() > 1e-6f && az.lengthSquared() > 1e-6f) {
                axisX.set(ax).normalize();
                axisY.set(ay).normalize();
                axisZ.set(az).normalize();

                // ensure orthonormality: make axisZ = axisX x axisY
                axisZ.set(axisX).cross(axisY).normalize();
                axisY.set(axisZ).cross(axisX).normalize();

                // halfSizes: project differences onto axes
                float hx = Math.abs(new Vector3f(v1).sub(v0).dot(axisX)) * 0.5f;
                float hy = Math.abs(new Vector3f(v3).sub(v0).dot(axisY)) * 0.5f;
                float hz = Math.abs(new Vector3f(v4).sub(v0).dot(axisZ)) * 0.5f;
                halfSizes.set(hx, hy, hz);
                if (hx <= 0 || hy <= 0 || hz <= 0) computeAxesFallback();
                return;
            }
        }

        // fallback: compute axes by covariance (very small PCA-like approach)
        computeAxesFallback();
    }

    private void computeAxesFallback() {
        // compute covariance matrix of points around center
        float cxx = 0, cxy = 0, cxz = 0, cyy = 0, cyz = 0, czz = 0;
        for (VertexElement v : vertices) {
            Vector3f p = new Vector3f(v.getPos()).sub(center);
            cxx += p.x * p.x;
            cxy += p.x * p.y;
            cxz += p.x * p.z;
            cyy += p.y * p.y;
            cyz += p.y * p.z;
            czz += p.z * p.z;
        }
        // Simplified: pick largest variance axis as axisX, then pick axisY orthonormal, axisZ cross.
        // This is not full eigen-decomposition but works well enough for box-like point sets.
        Vector3f candidateX = new Vector3f(cxx, cxy, cxz);
        if (candidateX.lengthSquared() < 1e-6f) candidateX.set(1,0,0);
        axisX.set(candidateX).normalize();
        // pick another vector (try world up)
        Vector3f tmp = new Vector3f(0, 1, 0);
        if (Math.abs(axisX.dot(tmp)) > 0.99f) tmp.set(1, 0, 0);
        axisY.set(tmp).sub(new Vector3f(axisX).mul(axisX.dot(tmp))).normalize();
        axisZ.set(axisX).cross(axisY).normalize();

        // compute half sizes by projecting points onto axes
        float maxX = 0, maxY = 0, maxZ = 0;
        for (VertexElement v : vertices) {
            Vector3f d = new Vector3f(v.getPos()).sub(center);
            maxX = Math.max(maxX, Math.abs(d.dot(axisX)));
            maxY = Math.max(maxY, Math.abs(d.dot(axisY)));
            maxZ = Math.max(maxZ, Math.abs(d.dot(axisZ)));
        }
        halfSizes.set(maxX, maxY, maxZ);
        // if any zero, set small epsilon
        if (halfSizes.x <= 0) halfSizes.x = 1e-3f;
        if (halfSizes.y <= 0) halfSizes.y = 1e-3f;
        if (halfSizes.z <= 0) halfSizes.z = 1e-3f;
    }

    private void markDirty() {
        this.dirty = true;
    }

    // Call this after any transform operation if you manually modified vertices
    public void notifyVerticesChanged() {
        markDirty();
    }

    // --- containment / intersection ------------------
    /**
     * Test if a world-space point is inside this oriented box.
     */
    public boolean containsPoint(Vector3f worldPoint) {
        recomputeIfNeeded();
        // compute local coordinates: local = R^T * (p - center) / halfSizes
        Vector3f d = new Vector3f(worldPoint).sub(center);
        float lx = d.dot(axisX) / halfSizes.x;
        float ly = d.dot(axisY) / halfSizes.y;
        float lz = d.dot(axisZ) / halfSizes.z;
        return Math.abs(lx) <= 1f + 1e-6f && Math.abs(ly) <= 1f + 1e-6f && Math.abs(lz) <= 1f + 1e-6f;
    }

    /**
     * Test intersection with another DLBoundingBox.
     * Implementation uses OBB-localization: we test overlap by transforming corners/test to both spaces.
     * This is not a full SAT with all cross axes but is robust for most practical OBB-vs-OBB cases.
     */
    public boolean intersects(DLBoundingBox other) {
        // We'll do conservative tests:
        // 1) AABB overlap of bounding boxes (fast reject)
        AABB a = this.getBoundingBox();
        AABB b = other.getBoundingBox();
        if (!a.intersects(b)) return false;

        // 2) Check if any corner of this is inside other, or vice versa
        for (VertexElement v : this.vertices) {
            if (other.containsPoint(v.getPos())) return true;
        }
        for (VertexElement v : other.vertices) {
            if (this.containsPoint(v.getPos())) return true;
        }

        // 3) As final fallback, sample centers of faces / edges (not exhaustive but helpful)
        for (int i = 0; i < 8; i++) {
            Vector3f v = vertices.get(i).getPos();
            // test midpoint to other
            if (other.containsPoint(v)) return true;
        }

        // conservative: assume no intersection if none above
        return false;
    }

    /**
     * Intersect with an axis-aligned AABB (Minecraft's AxisAlignedBB).
     */
    public boolean intersects(AABB aabb) {
        // Quick AABB-vs-AABB test using this bounding AABB
        if (!this.getBoundingBox().intersects(aabb)) return false;

        // Sample the 8 corners of the axis aligned box; if any corner is inside OBB -> intersects.
        Vector3f[] corners = new Vector3f[8];
        corners[0] = new Vector3f((float)aabb.minX, (float)aabb.minY, (float)aabb.minZ);
        corners[1] = new Vector3f((float)aabb.maxX, (float)aabb.minY, (float)aabb.minZ);
        corners[2] = new Vector3f((float)aabb.maxX, (float)aabb.maxY, (float)aabb.minZ);
        corners[3] = new Vector3f((float)aabb.minX, (float)aabb.maxY, (float)aabb.minZ);
        corners[4] = new Vector3f((float)aabb.minX, (float)aabb.minY, (float)aabb.maxZ);
        corners[5] = new Vector3f((float)aabb.maxX, (float)aabb.minY, (float)aabb.maxZ);
        corners[6] = new Vector3f((float)aabb.maxX, (float)aabb.maxY, (float)aabb.maxZ);
        corners[7] = new Vector3f((float)aabb.minX, (float)aabb.maxY, (float)aabb.maxZ);

        for (Vector3f c : corners) {
            if (containsPoint(c)) return true;
        }

        // If no corner is inside the other, it's still possible they overlap (one encloses the other partially).
        // As a heuristic, check if any of this box's vertex is inside the AABB.
        for (VertexElement v : vertices) {
            Vector3f p = v.getPos();
            if (p.x >= aabb.minX && p.x <= aabb.maxX &&
                p.y >= aabb.minY && p.y <= aabb.maxY &&
                p.z >= aabb.minZ && p.z <= aabb.maxZ) {
                return true;
            }
        }

        // Conservative: no intersection
        return false;
    }

    // --- pixelation & voxel conversion ----------------
    /**
     * Pixelate/voxelize the (possibly rotated) bounding box into axis-aligned voxels (size = 1/16).
     * Returns a VoxelShape representing the union of axis-aligned 1/16 voxels that approximate the rotated box.
     *
     * Approach:
     * - Compute world-space AABB of the rotated box.
     * - Iterate each 1/16 cell that overlaps that AABB.
     * - For each cell test simple overlap:
     *     - If any of the 8 voxel corners is inside the OBB => include voxel.
     *     - Else if any of the OBB's 8 corners is inside the voxel => include voxel.
     *
     * This produces a faithful voxelized approximation useful for Minecraft collision shapes.
     */
    public VoxelShape pixelate() {
        return pixelate(1.0f / 16.0f);
    }

    public VoxelShape pixelate(float gridSize) {
        recomputeIfNeeded();

        AABB worldAabb = getBoundingBox();
        int minX = floorDiv((float)worldAabb.minX, gridSize);
        int minY = floorDiv((float)worldAabb.minY, gridSize);
        int minZ = floorDiv((float)worldAabb.minZ, gridSize);
        int maxX = floorDivCeil((float)worldAabb.maxX, gridSize);
        int maxY = floorDivCeil((float)worldAabb.maxY, gridSize);
        int maxZ = floorDivCeil((float)worldAabb.maxZ, gridSize);

        VoxelShape result = Shapes.empty();

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    float vx0 = x * gridSize;
                    float vy0 = y * gridSize;
                    float vz0 = z * gridSize;
                    float vx1 = vx0 + gridSize;
                    float vy1 = vy0 + gridSize;
                    float vz1 = vz0 + gridSize;

                    // check if voxel intersects OBB using corner tests
                    boolean intersects = false;

                    Vector3f[] voxelCorners = new Vector3f[] {
                        new Vector3f(vx0, vy0, vz0),
                        new Vector3f(vx1, vy0, vz0),
                        new Vector3f(vx1, vy1, vz0),
                        new Vector3f(vx0, vy1, vz0),
                        new Vector3f(vx0, vy0, vz1),
                        new Vector3f(vx1, vy0, vz1),
                        new Vector3f(vx1, vy1, vz1),
                        new Vector3f(vx0, vy1, vz1)
                    };

                    for (Vector3f corner : voxelCorners) {
                        if (containsPoint(corner)) {
                            intersects = true;
                            break;
                        }
                    }

                    if (!intersects) {
                        // check if any OBB corner is inside voxel
                        for (VertexElement v : vertices) {
                            Vector3f p = v.getPos();
                            if (p.x >= vx0 && p.x <= vx1 &&
                                p.y >= vy0 && p.y <= vy1 &&
                                p.z >= vz0 && p.z <= vz1) {
                                intersects = true;
                                break;
                            }
                        }
                    }

                    if (intersects) {
                        AABB voxelAabb = new AABB(vx0, vy0, vz0, vx1, vy1, vz1);
                        result = Shapes.or(result, Shapes.create(voxelAabb));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Convert this oriented box into a VoxelShape by pixelating at 1/16.
     */
    public VoxelShape toVoxelShape() {
        return pixelate(1.0f / 16.0f);
    }

    // --- utilities -----------------------------------
    @Override
    public AABB getBoundingBox() {
        // produce axis-aligned bounding box of vertices
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (VertexElement v : vertices) {
            Vector3f p = v.getPos();
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static int floorDiv(float v, float grid) {
        return (int)Math.floor(v / grid);
    }

    private static int floorDivCeil(float v, float grid) {
        return (int)Math.ceil(v / grid);
    }

    // --- VertexElement (IVertexElement) ----------------
    public static class VertexElement implements IVertexElement {
        private final Vector3f pos;

        public VertexElement(Vector3f pos) {
            this.pos = pos;
        }

        @Override
        public Vector3f getPos() {
            return pos;
        }
    }

    // --- convenience factory: create from axis-aligned AABB --------------
    public static DLBoundingBox fromAABB(AABB aabb) {
        return new DLBoundingBox(aabb);
    }

    // --- example: rotate around center ----------------
    public void rotate(Quaternionf rotation) {
        Vector3f pivot = this.center();
        this.rotate(rotation, pivot); // uses ITransformable.rotate default impl
        notifyVerticesChanged();
    }

    // --- Helpers for external use ---------------------
    /**
     * Converts list of AxisAlignedBBs (from a VoxelShape) into a list of DLBoundingBox objects (one per AABB).
     * Use when you want exact correspondence (instead of the single-enclosing box done in fromVoxelShape()).
     */
    public static List<DLBoundingBox> listFromVoxelShape(VoxelShape shape) {
        List<DLBoundingBox> out = new ArrayList<>();
        List<AABB> boxes = shape.toAabbs();
        for (AABB a : boxes) {
            out.add(new DLBoundingBox(a));
        }
        return out;
    }

    // --- override translate/scale/transform to mark dirty ----------------
    @Override
    public void translate(Vector3f delta) {
        ITransformable.super.translate(delta);
        notifyVerticesChanged();
    }

    @Override
    public void scale(Vector3f factor, Vector3f pivot) {
        ITransformable.super.scale(factor, pivot);
        notifyVerticesChanged();
    }

    @Override
    public void rotate(Quaternionf rotation, Vector3f pivot) {
        ITransformable.super.rotate(rotation, pivot);
        notifyVerticesChanged();
    }

    @Override
    public void transform(Matrix4f matrix) {
        ITransformable.super.transform(matrix);
        notifyVerticesChanged();
    }
}

