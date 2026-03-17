package de.mrjulsen.wires;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.MapCache;
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

public class WireCollision {
    private final UUID connectionId;
    private final Map<String, WirePoints> points;
    private final BlockPos sectionOrigin;
    private final Set<ChunkPos> chunks = new HashSet<>();
    private final Multimap<BlockPos, WireBlockCollision> blocks = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Set<SectionPos> sections = new HashSet<>();

    private final MapCache<Double, String, String> lengthCache;

    public WireCollision(Multimap<ChunkPos, WireCollision> chunkMap, Multimap<SectionPos, WireCollision> sectionMap, Multimap<BlockPos, WireCollision> blockMap, UUID connectionId, BlockPos origin, Map<String, WirePoints> points) {
        chunkMap.values().removeIf(x -> x.getId().equals(connectionId));
        sectionMap.values().removeIf(x -> x.getId().equals(connectionId));
        blockMap.values().removeIf(x -> x.getId().equals(connectionId));

        this.connectionId = connectionId;
        this.sectionOrigin = SectionPos.of(origin).origin();
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
            Vector3d[] vec = p.getValue().vertices();
            Map<BlockPos, WireBlockCollision> positions = traceAlongWire(p.getKey(), vec, (float)(DragonLib.BLOCK_PIXEL * ModServerConfig.WIRE_COLLISION_TRACER_STEP_SIZE.get()), origin);
            for (Map.Entry<BlockPos, WireBlockCollision> pos : positions.entrySet()) {
                DLUtils.doIfNotNull(blockMap, x -> x.put(pos.getKey(), this));
                SectionPos section = SectionPos.of(pos.getKey());
                ChunkPos chunk = section.chunk();
                if (sections.add(section)) {
                    DLUtils.doIfNotNull(sectionMap, x -> x.put(section, this));
                }
                if (chunks.add(chunk)) {
                    DLUtils.doIfNotNull(chunkMap, x -> x.put(chunk, this));
                }
                this.blocks.put(pos.getKey(), pos.getValue());
            }
        }
    }

    public UUID getId() {
        return connectionId;
    }

    public Set<BlockPos> blocksIn() {
        return Collections.unmodifiableSet(blocks.keySet());
    }

    public Collection<WireBlockCollision> collisionsInBlock(BlockPos pos) {
        return Collections.unmodifiableCollection(blocks.get(pos));
    }

    public Set<SectionPos> sectionsIn() {
        return Collections.unmodifiableSet(sections);
    }

    public Collection<WireBlockCollision> getAllCollisions() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public double length(String wireName) {
        if (!points.containsKey(wireName)) {
            return 0;
        }
        return lengthCache.get(wireName, wireName);
    }

    public WirePoints getWirePointsOf(String wireName) {
        return this.points.get(wireName);
    }

    public double worldPosToWirePos(String name, Vector3d p) {
        Vector3d[] v = getWirePointsOf(name).vertices();
        double nextDistSqr = Float.MAX_VALUE;
        double lenNextPoint = 0f;

        double sum = 0f;

        Vector3d a = new Vector3d(v[0]).add(sectionOrigin.getX(), sectionOrigin.getY(), sectionOrigin.getZ());
        for (int i = 1; i < v.length; i++) {
            Vector3d b = new Vector3d(v[i]).add(sectionOrigin.getX(), sectionOrigin.getY(), sectionOrigin.getZ());

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

    public Vector3d wirePosToWorldPos(String name, double distanceOnWire) {
        Vector3d[] v = getWirePointsOf(name).vertices();
        Vector3d a = new Vector3d(v[0]).add(sectionOrigin.getX(), sectionOrigin.getY(), sectionOrigin.getZ());
        double sum = 0f;

        for (int i = 1; i < v.length; i++) {
            Vector3d b = new Vector3d(v[i]).add(sectionOrigin.getX(), sectionOrigin.getY(), sectionOrigin.getZ());
            Vector3d ab = new Vector3d(b).sub(a);
            double segmentLength = ab.length();

            if (distanceOnWire <= sum + segmentLength) {
                double t = (distanceOnWire - sum) / segmentLength;
                return new Vector3d(ab).mul(t).add(a);
            }

            sum += segmentLength;
            a = b;
        }
        return new Vector3d(v[v.length - 1]).add(sectionOrigin.getX(), sectionOrigin.getY(), sectionOrigin.getZ());
    }



    private Map<BlockPos, WireBlockCollision> traceAlongWire(String wireName, Vector3d[] points, double step, BlockPos origin) {
        if (points.length <= 1) {
            return Map.of();
        }
        
        BlockPos originSection = SectionPos.of(origin).origin();
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
                BlockPos newPos = new BlockPos((int)Math.floor(v.x), (int)Math.floor(v.y), (int)Math.floor(v.z)).offset(originSection);
                currentPoint = new Vector3d(originSection.getX() + v.x, originSection.getY() + v.y, originSection.getZ() + v.z);
                if (lastBlock == null || !lastBlock.equals(newPos)) {
                    if (lastBlock != null && lastPoint != null) {
                        final BlockPos fLastPos = lastBlock;
                        final Vector3d fLastPoint = lastPoint;
                        final Vector3d fCurrentPoint = currentPoint;
                        blocks.computeIfAbsent(fLastPos, x -> new WireBlockCollision(
                            this,
                            getId(),
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
                getId(),
                wireName,
                fLastPos,
                new Vector3d(fLastPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ())
            )).setSecondPoint(new Vector3d(fCurrentPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ()));
        }

        return blocks;
    }
    
	public static boolean connectionBlocked(Level level, BlockPos pos, BlockState state, Vector3d a, Vector3d b) {
		VoxelShape shape = state.getCollisionShape(level, pos);
		shape = Shapes.joinUnoptimized(shape, Shapes.block(), BooleanOp.AND);
		for(AABB aabb : shape.toAabbs()) {
			aabb = aabb.inflate(1e-5);
			if (aabb.contains(new Vec3(a.x(), a.y(), a.z())) || aabb.contains(new Vec3(b.x(), b.y(), b.z())) || aabb.clip(new Vec3(a.x(), a.y(), a.z()), new Vec3(b.x(), b.y(), b.z())).isPresent()) {
				return true;
            }
		}
		return false;
	}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WireCollision");
        for (WireBlockCollision c : blocks.values()) {
            sb.append("\n");
            sb.append(c.toString());
        }
        return sb.toString();
    }

    public static class WireBlockCollision {
        private final UUID id;
        private final WireCollision collision;
        private final String wireName;
        private final BlockPos pos;
        private final Vector3d entryPointA;
        private Vector3d entryPointB;

        private final Cache<Vector3d> absA;
        private final Cache<Vector3d> absB;

        public WireBlockCollision(WireCollision collision, UUID id, String wireName, BlockPos pos, Vector3d entryPointA) {
            this.id = id;
            this.collision = collision;
            this.wireName = wireName;
            this.pos = pos;
            this.entryPointA = new Vector3d(bounds(entryPointA.x), entryPointA.y, bounds(entryPointA.z));
            this.absA = new Cache<>(() -> new Vector3d(this.entryPointA).add(pos.getX(), pos.getY(), pos.getZ()));
            this.absB = new Cache<>(() -> new Vector3d(this.entryPointB).add(pos.getX(), pos.getY(), pos.getZ()));
        }

        public WireBlockCollision(WireCollision collision, UUID id, String wireName, BlockPos pos, Vector3d entryPointA, Vector3d entryPointB) {
            this(collision, id, wireName, pos, entryPointA);
            this.entryPointB = new Vector3d(bounds(entryPointB.x), entryPointB.y, bounds(entryPointB.z));
            this.absA.clear();
            this.absB.clear();
        }

        public UUID getId() {
            return id;
        }

        public WireCollision getCollision() {
            return collision;
        }

        public WireBlockCollision setSecondPoint(Vector3d oEntryPointB) {
            this.entryPointB = new Vector3d(bounds(oEntryPointB.x), oEntryPointB.y, bounds(oEntryPointB.z));
            this.absA.clear();
            this.absB.clear();
            return this;
        }

        private static float bounds(double v) {
            return (float)(v <= DragonLib.BLOCK_PIXEL ? 0 : (v >= 1D - DragonLib.BLOCK_PIXEL ? 1 : v));
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

        public BlockPos pos() {
            return pos;
        }

        public Vector3d entryPointA() {
            return entryPointA;
        }

        public Vector3d entryPointB() {
            return entryPointB;
        }

        public Vector3d absA() {
            return absA.get();
        }

        public Vector3d absB() {
            return absB.get();
        }

        public String wireName() {
            return wireName;
        }
    }
}
