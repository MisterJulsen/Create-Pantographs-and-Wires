package de.mrjulsen.wires.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3d;
import org.joml.Vector3d;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.MapCache;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.wires.WirePoints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class NewWireCollision {

    public static final double EPSILON = 1E-5;

    private final IWireGraph graph;
    private final UUID edgeId;
    private final Map<String, WirePoints> points;

    private final Set<ChunkPos> chunks = new HashSet<>();
    private final Set<SectionPos> sections = new HashSet<>();
    private final Multimap<BlockPos, WireBlockCollision> blockCollisions = MultimapBuilder.hashKeys().hashSetValues().build();

    private final MapCache<Double, String, String> lengthCache;

    public NewWireCollision(IWireGraph graph, UUID connectionId, Map<String, WirePoints> points) {
        this.graph = graph;
        this.edgeId = connectionId;
        this.points = ImmutableMap.copyOf(points);
        this.lengthCache = new MapCache<>((name) -> {
            WirePoints p = points.get(name);
            double len = 0;
            Vector3d a = p.vertices()[0];
            for (int i = 1; i < p.vertices().length; i++) {
                Vector3d b = p.vertices()[i];
                len += b.distance(a);
                a = b;
            }
            return len;
        }, Object::hashCode);

        for (Map.Entry<String, WirePoints> p : points.entrySet()) {
            Map<BlockPos, WireBlockCollision> positions = traceAlongWire(p.getKey(), p.getValue(), (double)DragonLib.BLOCK_PIXEL * ModServerConfig.WIRE_COLLISION_TRACER_STEP_SIZE.get());
            for (Map.Entry<BlockPos, WireBlockCollision> pos : positions.entrySet()) {
                SectionPos section = SectionPos.of(pos.getKey());
                ChunkPos chunk = section.chunk();
                sections.add(section);
                chunks.add(chunk);
                this.blockCollisions.put(pos.getKey(), pos.getValue());
            }
        }
    }

    public UUID getId() {
        return edgeId;
    }

    public IWireGraph getGraph() {
        return graph;
    }

    /**
     * @return A list of blocks through which the wire passes
     */
    public Set<BlockPos> blocksIn() {
        return Collections.unmodifiableSet(blockCollisions.keySet());
    }

    /**
     * A list of all block collisions that exist in the specified block. Depending on the wire type,
     * the list can contain one object or multiple objects (e.g. for wire connections consisting of
     * multiple individual wires).
     * @param pos The position from which the collisions should be retrieved
     * @return A collection of collisions
     */
    public Collection<WireBlockCollision> collisionsInBlock(BlockPos pos) {
        return Collections.unmodifiableCollection(blockCollisions.get(pos));
    }

    /**
     * @return A list of sections through which the wire passes
     */
    public Set<SectionPos> sectionsIn() {
        return Collections.unmodifiableSet(sections);
    }

    /**
     * @return A list of sections through which the wire passes
     */
    public Set<ChunkPos> chunksIn() {
        return Collections.unmodifiableSet(chunks);
    }


    /**
     * @return All block collisions
     */
    public Collection<WireBlockCollision> getAllCollisions() {
        return Collections.unmodifiableCollection(blockCollisions.values());
    }

    /**
     * @param wireName The name of the wire.
     * @return The length of the specified wire.
     */
    public double length(String wireName) {
        if (!points.containsKey(wireName)) {
            return 0;
        }
        return lengthCache.get(wireName, wireName);
    }

    public boolean hasWire(String wireName) {
        return this.points.containsKey(wireName);
    }

    /**
     * @param wireName The name of the wire.
     * @return All points that define the wire.
     */
    public WirePoints getWirePointsOf(String wireName) {
        return this.points.get(wireName);
    }

    public boolean debug_hasWirePoints(String wireName) {
        return this.points.containsKey(wireName);
    }

    /**
     * Converts the specified world coordinates to one-dimensional wire coordinates (starting from the
     * origin of the wire, which is the first node). If the world position isn't exactly on
     * the wire, the position closest to the world position is used.
     * @param name The name of the wire.
     * @param p The world position.
     * @return The position on the wire (or distance from the wire's origin).
     */
    public double worldPosToWirePos(String name, Vector3d p) {
        Vector3d[] v = getWirePointsOf(name).vertices();
        double nextDistSqr = Float.MAX_VALUE;
        double lenNextPoint = 0f;

        double sum = 0f;

        Vector3d a = new Vector3d(v[0]);
        for (int i = 1; i < v.length; i++) {
            Vector3d b = new Vector3d(v[i]);

            Vector3d ab = new Vector3d(b).sub(a);
            Vector3d ap = new Vector3d(p).sub(a);

            double segmentLength = ab.length();
            double t = ab.dot(ap) / ab.lengthSquared();
            t = Math.max(0, Math.min(1, t));
            Vector3d nextPoint = new Vector3d(ab).mul(t).add(a);

            double distSqr = new Vector3d(p).sub(nextPoint).lengthSquared();

            if (distSqr < nextDistSqr) {
                nextDistSqr = distSqr;
                lenNextPoint = sum + segmentLength * t;
            }

            sum += segmentLength;
            a = b;
        }

        return lenNextPoint;
    }


    /**
     * Converts the specified wire coordinate to world coordinates (starting from the
     * origin of the wire, which is the first node).
     * @param name The name of the wire.
     * @param distanceOnWire The position on the wire.
     * @return The world position.
     */
    public Vector3d wirePosToWorldPos(String name, double distanceOnWire) {
        if (!debug_hasWirePoints(name)) {
            return new Vector3d();
        }
        Vector3d[] v = getWirePointsOf(name).vertices();
        Vector3d a = new Vector3d(v[0]);
        double sum = 0f;

        for (int i = 1; i < v.length; i++) {
            Vector3d b = new Vector3d(v[i]);
            Vector3d ab = new Vector3d(b).sub(a);
            double segmentLength = ab.length();

            if (distanceOnWire <= sum + segmentLength) {
                double t = (distanceOnWire - sum) / segmentLength;
                return new Vector3d(ab).mul(t).add(a);
            }

            sum += segmentLength;
            a = b;
        }
        return new Vector3d(v[v.length - 1]);
    }


    /**
     * Traces along the wire and collects all {@link BlockPos} through which the wire passes. For each Block Position,
     * a new {@link WireBlockCollision} object is created with detailed information about the collision in that block
     * (e.g. where the wire enters and leaves).
     * @param wireName The name of the wire (used for the {@link WireBlockCollision})
     * @param packedPoints The points of the wire.
     * @param step The step size.
     * @return 
     */
    private Map<BlockPos, WireBlockCollision> traceAlongWire(String wireName, WirePoints packedPoints, double step) {
        Vector3d[] points = packedPoints.vertices();        
        if (points.length <= 1) {
            return Map.of();
        }
        
        Map<BlockPos, WireBlockCollision> blocks = new HashMap<>();
        Vector3d prevVec = points[0];

        BlockPos lastBlock = null;
        Vector3d lastPoint = null;
        Vector3d currentPoint = null;

        for (int i = 1; i < points.length; i++) {
            Vector3d vec = points[i];
            Vector3d delta = new Vector3d(vec).sub(prevVec);
            Vector3d normalized = new Vector3d(delta).normalize();
            double length = delta.length();
            int c = (int)Math.ceil(length / step);


            for (int k = 0; k <= c; k++) {
                Vector3d v = new Vector3d(prevVec).add(new Vector3d(normalized).mul(k * step));
                BlockPos newPos = new BlockPos((int)Math.floor(v.x), (int)Math.floor(v.y), (int)Math.floor(v.z));
                currentPoint = new Vector3d(v.x, v.y, v.z);
                if (lastBlock == null || !lastBlock.equals(newPos)) {
                    if (lastBlock != null && lastPoint != null) {
                        final BlockPos fLastPos = lastBlock;
                        final Vector3d fLastPoint = lastPoint;
                        final Vector3d fCurrentPoint = currentPoint;
                        blocks.computeIfAbsent(fLastPos, x -> new WireBlockCollision(
                            this,
                            wireName,
                            fLastPos,
                            new Vector3d(fLastPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ())
                        )).setSecondPoint(new Vector3d(fCurrentPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ()));
                    }
                    
                    lastPoint = currentPoint;
                    lastBlock = newPos;
                }
            }
            prevVec = vec;
        }

        if (lastBlock != null && lastPoint != null) {
            final BlockPos fLastPos = lastBlock;
            final Vector3d fLastPoint = lastPoint;
            final Vector3d fCurrentPoint = currentPoint;
            blocks.computeIfAbsent(fLastPos, x -> new WireBlockCollision(
                this,
                wireName,
                fLastPos,
                new Vector3d(fLastPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ())
            )).setSecondPoint(new Vector3d(fCurrentPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ()));
        }

        return blocks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        for (WireBlockCollision c : blockCollisions.values()) {
            sb.append("\n");
            sb.append(c.toString());
        }
        return sb.toString();
    }
    


	public static boolean isConnectionBlocked(Level level, BlockPos pos, BlockState state, Vector3d a, Vector3d b) {
		VoxelShape shape = state.getCollisionShape(level, pos);
		shape = Shapes.joinUnoptimized(shape, Shapes.block(), BooleanOp.AND);
		for(AABB aabb : shape.toAabbs()) {
			aabb = aabb.inflate(EPSILON);
			if (aabb.contains(new Vec3(a.x(), a.y(), a.z())) || aabb.contains(new Vec3(b.x(), b.y(), b.z())) || aabb.clip(new Vec3(a.x(), a.y(), a.z()), new Vec3(b.x(), b.y(), b.z())).isPresent()) {
				return true;
            }
		}
		return false;
	}


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NewWireCollision o) {
            return edgeId.equals(o.edgeId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return edgeId.hashCode();
    }






    /**
     * The wire collision in a specific block, with information such as the in and out vector.
     */
    public static class WireBlockCollision {
        private final String wireName;
        private final NewWireCollision collisionRef;
        private final BlockPos pos; // The block where the collision is

        private final Vector3d entryPointA; // relative pos
        private Vector3d entryPointB; // relative pos

        private final Cache<Vector3d> absA;
        private final Cache<Vector3d> absB;

        public WireBlockCollision(NewWireCollision collisionRef, String wireName, BlockPos pos, Vector3d entryPointA) {
            this.collisionRef = collisionRef;
            this.wireName = wireName;
            this.pos = pos;
            // Korrektur für float Ungenauigkeiten
            this.entryPointA = new Vector3d(bounds(entryPointA.x), entryPointA.y, bounds(entryPointA.z));
            this.absA = new Cache<>(() -> new Vector3d(this.entryPointA).add(pos.getX(), pos.getY(), pos.getZ()));
            this.absB = new Cache<>(() -> new Vector3d(this.entryPointB).add(pos.getX(), pos.getY(), pos.getZ()));
        }

        public WireBlockCollision(NewWireCollision collision, String wireName, BlockPos pos, Vector3d entryPointA, Vector3d entryPointB) {
            this(collision, wireName, pos, entryPointA);
            this.entryPointB = new Vector3d(bounds(entryPointB.x), entryPointB.y, bounds(entryPointB.z));
            this.absA.clear();
            this.absB.clear();
        }

        private static float bounds(double v) {
            return (float)(v <= DragonLib.BLOCK_PIXEL ? 0 : (v >= 1D - DragonLib.BLOCK_PIXEL ? 1 : v));
        }

        public WireBlockCollision setSecondPoint(Vector3d oEntryPointB) {
            this.entryPointB = new Vector3d(bounds(oEntryPointB.x), oEntryPointB.y, bounds(oEntryPointB.z));
            this.absA.clear();
            this.absB.clear();
            return this;
        }
        

        /**
         * @return The reference to the entire {@link NewWireCollision} data. This object only contains detailed information about the collision in a specific block, not the entire wire.
         */
        public NewWireCollision getCollision() {
            return collisionRef;
        }

        /**
         * @return The {@link BlockPos} of the block for which this collision data is valid.
         */
        public BlockPos getBlockPos() {
            return pos;
        }

        /**
         * @return The relative (block) position where the wire enters the block.
         */
        public Vector3d getInVector() {
            return entryPointA;
        }

        /**
         * @return The relative (block) position where the wire leaves the block.
         */
        public Vector3d getOutVector() {
            return entryPointB;
        }

        /**
         * @return The absolute (world) position where the wire enters the block.
         */
        public Vector3d getAbsoluteInVector() {
            return absA.get();
        }

        /**
         * @return The absolute (world) position where the wire leaves the block.
         */
        public Vector3d getAbsoluteOutVector() {
            return absB.get();
        }

        /**
         * @return The name of the wire this collision data belongs to.
         */
        public String getWireName() {
            return wireName;
        }

        @Override
        public final String toString() {
            return String.format("%s (in: %s, out: %s)", pos, entryPointA, entryPointB);
        }

        @Override
        public final int hashCode() {
            return Objects.hash(pos, entryPointA, entryPointB);
        }

        @Override
        public final boolean equals(Object other) {
            if (other instanceof WireBlockCollision o) {
                return pos.equals(o.pos) && entryPointA.equals(o.entryPointA) && entryPointB.equals(o.entryPointB);
            }
            return false;
        }
    }
}
