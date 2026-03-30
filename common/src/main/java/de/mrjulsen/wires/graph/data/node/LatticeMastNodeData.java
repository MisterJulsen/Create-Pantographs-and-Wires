package de.mrjulsen.wires.graph.data.node;

import java.util.Objects;
import java.util.Optional;

import de.mrjulsen.paw.registry.ModBlockTags;
import org.joml.Vector3d;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.item.CustomData;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class LatticeMastNodeData extends NodeData implements INodeDataBlock {

    private static final String NBT_POS = "Pos";
    private BlockPos pos;

    public LatticeMastNodeData() {}

    public LatticeMastNodeData(BlockPos pos) {
        this.pos = pos;
    }
    
    @Override
    public BlockPos getBlockPos() {
        return pos;
    }
    
    @Override
    public NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> getRegistryType() {
        return (NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>>)(Object)WiresApi.LATTICE_MAST;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        Utils.putNbtBlockPos(nbt, NBT_POS, pos);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        this.pos = Utils.getNbtBlockPos(nbt, NBT_POS);
    }
    
    @Override
    public WireNode getOrCreateNode(WireGraph graph) {
        if (graph.getLevel().isLoaded(pos) && graph.getLevel().getBlockState(pos).getTags().anyMatch(x -> x.equals(ModBlockTags.CATENARY_HEADSPAN_CONNECTABLE))) {
            return graph.createNode(this, new Vector3d(getBlockPos().getCenter().x(), getBlockPos().getCenter().y(), getBlockPos().getCenter().z()));
        }
        return null;
    }
    
    @Override
    public Optional<ConnectorDataProvider> getConnectorCustomData(WireGraph graph, CustomData customData, WireNode node, int pointIndex) {
        return Optional.of(new ConnectorDataProvider.Empty());
    }

    @Override
    public Vector3d toWorldPos(IWireGraph graph) {
        return new Vector3d(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
    }

    @Override
    public boolean validate(WireGraph graph, CompoundTag currentItemData, int pointIndex) {
        return !graph.getLevel().isLoaded(pos) || graph.getLevel().getBlockState(pos).getTags().anyMatch(x -> x.equals(ModBlockTags.CATENARY_HEADSPAN_CONNECTABLE));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LatticeMastNodeData o) {
            return getBlockPos().equals(o.getBlockPos());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBlockPos());
    }
    
}
