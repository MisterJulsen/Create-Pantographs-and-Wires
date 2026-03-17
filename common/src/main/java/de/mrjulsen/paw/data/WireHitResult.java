package de.mrjulsen.paw.data;

import java.util.Optional;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import de.mrjulsen.wires.graph.NewWireCollision;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.network.WireId;
import de.mrjulsen.wires.util.GraphId;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class WireHitResult extends HitResult {

    private static final String NBT_VECTOR = "Vector";
    private static final String NBT_WIRE_POS = "PosOnWire";
    private static final String NBT_POS = "Pos";
    private static final String NBT_GRAPH_ID = "GraphId";
    private static final String NBT_WIRE = "Wire";

    private final BlockPos blockPos;
    private final double posOnWire;
    private final GraphId graphId;
    private final WireId wire;

    public WireHitResult(Vec3 location, double posOnWire, BlockPos pos, GraphId graphId, WireId wire) {
        super(location);
        this.blockPos = pos;
        this.posOnWire = posOnWire;
        this.graphId = graphId;
        this.wire = wire;
    }

    public BlockPos getBlockPos() {        
        return blockPos;
    }

    public double getPosOnWire() {
        return posOnWire;
    }

    public WireId getWireId() {
        return wire;
    }

    public GraphId getGraphId() {
        return graphId;
    }

    public double getWireLength(Level level) {
        return getCollision(level).map(x -> x.length(getWireId().name())).orElse(0D);
    }

    public double getPosPercentage(Level level) {
        return getCollision(level).map(x -> MathUtils.clamp(1D / x.length(getWireId().name()) * getPosOnWire(), 0D, 1D)).orElse(0D);
    }

    public Optional<NewWireCollision> getCollision(Level level) {        
        Optional<NewWireCollision> collision = Optional.empty();
        if (level.isClientSide()) {
            WireGraphClient graph = WireGraphManager.getClient(level, getGraphId());
            collision = graph.getCollisionById(getWireId().id());
        } else if (DragonLib.hasServer()) {
            WireGraph graph = WireGraphManager.get(level, getGraphId());
            collision = graph.getCollisionById(getWireId().id());
        }
        return collision;
    }

    @Override
    public Type getType() {
        return CustomHitResultTypes.WIRE.getType();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        Utils.putNbtVector3d(nbt, NBT_VECTOR, new Vector3d(getLocation().x(), getLocation().y(), getLocation().z()));
        nbt.putDouble(NBT_WIRE_POS, posOnWire);
        nbt.putString(NBT_GRAPH_ID, graphId.id());
        Utils.putNbtBlockPos(nbt, NBT_POS, blockPos);
        nbt.put(NBT_WIRE, wire.toNbt());
        return nbt;
    }

    public static Optional<WireHitResult> fromNbt(CompoundTag nbt) {
        Vector3d pos = Utils.getNbtVector3d(nbt, NBT_VECTOR);
        return WireId.fromNbt(nbt.getCompound(NBT_WIRE)).map(x -> new WireHitResult(
            new Vec3(pos.x(), pos.y(), pos.z()),
            nbt.getDouble(NBT_WIRE_POS),
            Utils.getNbtBlockPos(nbt, NBT_POS),
            new GraphId(nbt.getString(NBT_GRAPH_ID)),
            x
        ));
    }
}

