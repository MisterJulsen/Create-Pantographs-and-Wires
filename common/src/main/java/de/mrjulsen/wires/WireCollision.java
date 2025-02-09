package de.mrjulsen.wires;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.DLUtils;
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
    private final Set<ChunkPos> chunks = new HashSet<>();
    private final Multimap<BlockPos, WireBlockCollision> blocks = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Set<SectionPos> sections = new HashSet<>();

    public WireCollision(Multimap<ChunkPos, WireCollision> chunkMap, Multimap<SectionPos, WireCollision> sectionMap, Multimap<BlockPos, WireCollision> blockMap, UUID connectionId, BlockPos origin, Set<WirePoints> points) {
        chunkMap.values().removeIf(x -> x.getId().equals(connectionId));
        sectionMap.values().removeIf(x -> x.getId().equals(connectionId));
        blockMap.values().removeIf(x -> x.getId().equals(connectionId));

        this.connectionId = connectionId;
        for (WirePoints p : points) {
            Vector3f[] vec = p.vertices();
            Map<BlockPos, WireBlockCollision> positions = traceAlongWire(vec, (float)(WiresApi.PIXEL * ModServerConfig.WIRE_COLLISION_TRACER_STEP_SIZE.get()), origin);
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
        WireNetwork.clearConnectionCaches();
    }

    public UUID getId() {
        return connectionId;
    }

    public Set<BlockPos> blocksIn() {
        return blocks.keySet();
    }

    public Collection<WireBlockCollision> collisionsInBlock(BlockPos pos) {
        return blocks.get(pos);
    }

    public Set<SectionPos> sectionsIn() {
        return sections;
    }

    public Collection<WireBlockCollision> getAllCollisions() {
        return blocks.values();
    }

    private Map<BlockPos, WireBlockCollision> traceAlongWire(Vector3f[] points, float step, BlockPos origin) {
        if (points.length <= 1) {
            return Map.of();
        }
        
        BlockPos originSection = SectionPos.of(origin).origin();
        Map<BlockPos, WireBlockCollision> blocks = new HashMap<>();
        Vector3f prevVec = points[0];

        BlockPos lastBlock = null;
        Vector3f lastPoint = null;
        Vector3f currentPoint = null;

        for (int i = 1; i < points.length; i++) {
            Vector3f vec = points[i];
            Vector3f delta = new Vector3f(vec).sub(prevVec);
            Vector3f normalized = new Vector3f(delta).normalize();
            double length = delta.length();
            int c = (int)Math.ceil(length / step);


            for (int k = 0; k <= c; k++) {
                Vector3f v = new Vector3f(prevVec).add(new Vector3f(normalized).mul(k * step));
                BlockPos newPos = new BlockPos((int)Math.floor(v.x), (int)Math.floor(v.y), (int)Math.floor(v.z)).offset(originSection);
                currentPoint = new Vector3f(originSection.getX() + v.x, originSection.getY() + v.y, originSection.getZ() + v.z);
                if (lastBlock == null || !lastBlock.equals(newPos)) {
                    if (lastBlock != null && lastPoint != null) {
                        final BlockPos fLastPos = lastBlock;
                        final Vector3f fLastPoint = lastPoint;
                        final Vector3f fCurrentPoint = currentPoint;
                        blocks.computeIfAbsent(fLastPos, x -> new WireBlockCollision(
                            fLastPos,
                            new Vector3f(fLastPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ())
                        )).setSecondPoint(new Vector3f(fCurrentPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ()));
                    }
                    
                    lastPoint = currentPoint;
                    lastBlock = newPos;
                }
            }
            prevVec = vec;
        }

        if (lastBlock != null && lastPoint != null) {
            final BlockPos fLastPos = lastBlock;
            final Vector3f fLastPoint = lastPoint;
            final Vector3f fCurrentPoint = currentPoint;
            blocks.computeIfAbsent(fLastPos, x -> new WireBlockCollision(
                fLastPos,
                new Vector3f(fLastPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ())
            )).setSecondPoint(new Vector3f(fCurrentPoint).sub(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ()));
        }

        return blocks;
    }
    
	public static boolean connectionBlocked(Level level, BlockPos pos, BlockState state, Vector3f a, Vector3f b) {
		VoxelShape shape = state.getCollisionShape(level, pos);
		shape = Shapes.joinUnoptimized(shape, Shapes.block(), BooleanOp.AND);
		for(AABB aabb : shape.toAabbs()) {
			aabb = aabb.inflate(1e-5);
			if (aabb.contains(new Vec3(a)) || aabb.contains(new Vec3(b)) || aabb.clip(new Vec3(a), new Vec3(b)).isPresent()) {
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
        private final BlockPos pos;
        private final Vector3f entryPointA;
        private Vector3f entryPointB;

        private final Cache<Vector3f> absA;
        private final Cache<Vector3f> absB;

        public WireBlockCollision(BlockPos pos, Vector3f entryPointA) {
            this.pos = pos;
            this.entryPointA = new Vector3f(bounds(entryPointA.x), entryPointA.y, bounds(entryPointA.z));
            this.absA = new Cache<>(() -> new Vector3f(this.entryPointA).add(pos.getX(), pos.getY(), pos.getZ()));
            this.absB = new Cache<>(() -> new Vector3f(this.entryPointB).add(pos.getX(), pos.getY(), pos.getZ()));
        }

        public WireBlockCollision(BlockPos pos, Vector3f entryPointA, Vector3f entryPointB) {
            this(pos, entryPointA);
            this.entryPointB = new Vector3f(bounds(entryPointB.x), entryPointB.y, bounds(entryPointB.z));
            this.absA.clear();
            this.absB.clear();
        }

        public WireBlockCollision setSecondPoint(Vector3f oEntryPointB) {
            this.entryPointB = new Vector3f(bounds(oEntryPointB.x), oEntryPointB.y, bounds(oEntryPointB.z));
            this.absA.clear();
            this.absB.clear();
            return this;
        }

        private static float bounds(double v) {
            return (float)(v <= WiresApi.PIXEL ? 0 : (v >= 1D - WiresApi.PIXEL ? 1 : v));
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

        public Vector3f entryPointA() {
            return entryPointA;
        }

        public Vector3f entryPointB() {
            return entryPointB;
        }

        public Vector3f absA() {
            return absA.get();
        }

        public Vector3f absB() {
            return absB.get();
        }
    }
}
