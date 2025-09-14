package de.mrjulsen.wires.graph.data.node;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.item.WireBaseItem.CustomData;
import de.mrjulsen.wires.util.NodeId;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class BlockConnectorNodeData extends NodeData {

    private static final String NBT_POS = "Pos";
    
    private BlockPos pos;

    public BlockConnectorNodeData() {}

    public BlockConnectorNodeData(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }
    
    @Override
    public NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> getRegistryType() {
        return (NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>>)(Object)WiresApi.BLOCK_CONNECTOR;
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
        if (graph.getLevel().getBlockEntity(getPos()) instanceof WireConnectorBlockEntity be && graph.getLevel().getBlockState(getPos()).getBlock() instanceof IWireConnector) {
            WireNode node = null;
            if (be.getNodeId() == null || !graph.hasNode(be.getNodeId().id())) {
                node = graph.createNode(this, new Vector3f(getPos().getX(), getPos().getY(), getPos().getZ()));
                be.setNodeId(new NodeId(node.getId(), graph.getId()));
            } else {
                node = graph.getNode(be.getNodeId().id());
            }
            return node;
        }        
        return graph.createNode(this, new Vector3f(getPos().getX(), getPos().getY(), getPos().getZ()));
    }
    
    @Override
    public Optional<ConnectorDataProvider> getConnectorCustomData(WireGraph graph, CustomData customData, WireNode node, int pointIndex) {
        if (graph.getLevel().isLoaded(getPos()) && graph.getLevel().getBlockEntity(getPos()) instanceof WireConnectorBlockEntity && graph.getLevel().getBlockState(getPos()).getBlock() instanceof IWireConnector connector) {
            return Optional.of(connector.getConnectorData(graph.getLevel(), getPos(), customData, pointIndex));
        }
        return Optional.empty();
    }

    @Override
    public Vector3f toWorldPos(WireGraph graph) {
        return new Vector3f(getPos().getX(), getPos().getY(), getPos().getZ());
    }

    @Override
    public boolean validate(WireGraph graph, CompoundTag currentItemData, int pointIndex) {
        return graph.getLevel().isLoaded(getPos()) && graph.getLevel().getBlockEntity(getPos()) instanceof WireConnectorBlockEntity;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockConnectorNodeData o) {
            return getPos().equals(o.getPos());
        }
        return false;
    }    
}
