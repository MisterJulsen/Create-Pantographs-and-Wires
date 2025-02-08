package de.mrjulsen.wires;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.util.Const;
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
            Vec3[] vec = p.vertices();
            Map<BlockPos, WireBlockCollision> positions = traceAlongWire(vec, (float)(Const.PIXEL * ModServerConfig.WIRE_COLLISION_TRACER_STEP_SIZE.get()), origin);
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

    private Map<BlockPos, WireBlockCollision> traceAlongWire(Vec3[] points, float step, BlockPos origin) {
        if (points.length <= 1) {
            return Map.of();
        }
        
        BlockPos originSection = SectionPos.of(origin).origin();
        Map<BlockPos, WireBlockCollision> blocks = new HashMap<>();
        Vec3 prevVec = points[0];

        BlockPos lastBlock = null;
        Vec3 lastPoint = null;
        Vec3 currentPoint = null;

        for (int i = 1; i < points.length; i++) {
            Vec3 vec = points[i];
            Vec3 delta = vec.subtract(prevVec);
            Vec3 normalized = delta.normalize();
            double length = delta.length();
            int c = (int)Math.ceil(length / step);


            for (int k = 0; k <= c; k++) {
                Vec3 v = prevVec.add(normalized.scale(k * step));
                BlockPos newPos = new BlockPos((int)Math.floor(v.x), (int)Math.floor(v.y), (int)Math.floor(v.z)).offset(originSection);
                currentPoint = new Vec3(originSection.getX() + v.x, originSection.getY() + v.y, originSection.getZ() + v.z);
                if (lastBlock == null || !lastBlock.equals(newPos)) {
                    if (lastBlock != null && lastPoint != null) {
                        final BlockPos fLastPos = lastBlock;
                        final Vec3 fLastPoint = lastPoint;
                        final Vec3 fCurrentPoint = currentPoint;
                        blocks.computeIfAbsent(fLastPos, x -> new WireBlockCollision(
                            fLastPos,
                            fLastPoint.subtract(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ())
                        )).setSecondPoint(fCurrentPoint.subtract(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ()));
                    }
                    
                    lastPoint = currentPoint;
                    lastBlock = newPos;
                }
            }
            prevVec = vec;
        }

        if (lastBlock != null && lastPoint != null) {
            final BlockPos fLastPos = lastBlock;
            final Vec3 fLastPoint = lastPoint;
            final Vec3 fCurrentPoint = currentPoint;
            blocks.computeIfAbsent(fLastPos, x -> new WireBlockCollision(
                fLastPos,
                fLastPoint.subtract(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ())
            )).setSecondPoint(fCurrentPoint.subtract(fLastPos.getX(), fLastPos.getY(), fLastPos.getZ()));
        }

        return blocks;
    }
    
	public static boolean connectionBlocked(Level level, BlockPos pos, BlockState state, Vec3 a, Vec3 b) {
		VoxelShape shape = state.getCollisionShape(level, pos);
		shape = Shapes.joinUnoptimized(shape, Shapes.block(), BooleanOp.AND);
		for(AABB aabb : shape.toAabbs()) {
			aabb = aabb.inflate(1e-5);
			if (aabb.contains(a) || aabb.contains(b) || aabb.clip(a, b).isPresent()) {
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
        private final Vec3 entryPointA;
        private Vec3 entryPointB;

        private final Cache<Vec3> absA;
        private final Cache<Vec3> absB;

        public WireBlockCollision(BlockPos pos, Vec3 entryPointA) {
            this.pos = pos;
            this.entryPointA = new Vec3(bounds(entryPointA.x), entryPointA.y, bounds(entryPointA.z));
            this.absA = new Cache<>(() -> this.entryPointA.add(pos.getX(), pos.getY(), pos.getZ()));
            this.absB = new Cache<>(() -> this.entryPointB.add(pos.getX(), pos.getY(), pos.getZ()));
        }

        public WireBlockCollision(BlockPos pos, Vec3 entryPointA, Vec3 entryPointB) {
            this(pos, entryPointA);
            this.entryPointB = new Vec3(bounds(entryPointB.x), entryPointB.y, bounds(entryPointB.z));
            this.absA.clear();
            this.absB.clear();
        }

        public WireBlockCollision setSecondPoint(Vec3 oEntryPointB) {
            this.entryPointB = new Vec3(bounds(oEntryPointB.x), oEntryPointB.y, bounds(oEntryPointB.z));
            this.absA.clear();
            this.absB.clear();
            return this;
        }

        private static double bounds(double v) {
            return v <= Const.PIXEL ? 0 : (v >= 1D - Const.PIXEL ? 1 : v);
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

        public Vec3 entryPointA() {
            return entryPointA;
        }

        public Vec3 entryPointB() {
            return entryPointB;
        }

        public Vec3 absA() {
            return absA.get();
        }

        public Vec3 absB() {
            return absB.get();
        }
    }
}
