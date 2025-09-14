package de.mrjulsen.wires.graph.data.node;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.item.WireBaseItem.CustomData;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class LatticeMastNodeData extends NodeData {

    private static final String NBT_POS = "Pos";
    private BlockPos pos;

    public LatticeMastNodeData() {}

    public LatticeMastNodeData(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
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
        if (graph.getLevel().isLoaded(pos) && graph.getLevel().getBlockState(pos).getTags().anyMatch(x -> x.equals(ModBlocks.TAG_CATENARY_HEADSPAN_CONNECTABLE))) {
            return graph.createNode(this, getPos().getCenter().toVector3f());
        }
        return null;
    }
    
    @Override
    public Optional<ConnectorDataProvider> getConnectorCustomData(WireGraph graph, CustomData customData, WireNode node, int pointIndex) {
        return Optional.of(new ConnectorDataProvider.Empty());
    }

    @Override
    public Vector3f toWorldPos(WireGraph graph) {
        return new Vector3f(getPos().getX(), getPos().getY(), getPos().getZ());
    }

    @Override
    public boolean validate(WireGraph graph, CompoundTag currentItemData, int pointIndex) {
        return graph.getLevel().isLoaded(pos) && graph.getLevel().getBlockState(pos).getTags().anyMatch(x -> x.equals(ModBlocks.TAG_CATENARY_HEADSPAN_CONNECTABLE));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LatticeMastNodeData o) {
            return getPos().equals(o.getPos());
        }
        return false;
    }
    
}
