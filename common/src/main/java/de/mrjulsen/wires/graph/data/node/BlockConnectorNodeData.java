package de.mrjulsen.wires.graph.data.node;

import java.util.Objects;
import java.util.Optional;

import de.mrjulsen.paw.components.WireConnectionDataComponent;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.config.ModCommonConfig;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.item.CustomData;
import de.mrjulsen.wires.util.NodeId;
import de.mrjulsen.wires.util.SafeChunkUtils;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class BlockConnectorNodeData extends NodeData {

    private static final String NBT_POS = "Pos";
    private static final String NBT_PENDING = "IsPending";
    
    private BlockPos pos;
    private boolean pending;

    public BlockConnectorNodeData() {}

    public BlockConnectorNodeData(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isPending() {
        return pending;
    }
    
    @Override
    public NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> getRegistryType() {
        return (NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>>)(Object)WiresApi.BLOCK_CONNECTOR;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        Utils.putNbtBlockPos(nbt, NBT_POS, pos);
        if (pending) nbt.putBoolean(NBT_PENDING, pending);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        this.pos = Utils.getNbtBlockPos(nbt, NBT_POS);
        this.pending = nbt.getBoolean(NBT_PENDING);
    }
    
    @Override
    public WireNode getOrCreateNode(WireGraph graph) {
        if (graph.getLevel().isLoaded(getPos()) && graph.getLevel().getBlockEntity(getPos()) instanceof WireConnectorBlockEntity be && graph.getLevel().getBlockState(getPos()).getBlock() instanceof IWireConnector) {
            WireNode node = null;
            if (be.getNodeId() == null || !graph.hasNode(be.getNodeId().id())) {
                node = graph.createNode(this, new Vector3d(getPos().getX(), getPos().getY(), getPos().getZ()));
                be.setNodeId(new NodeId(node.getId(), graph.getId()));
            } else {
                node = graph.getNode(be.getNodeId().id());
            }
            return node;
        }
        pending = true;
        return graph.createNode(this, new Vector3d(getPos().getX(), getPos().getY(), getPos().getZ()));
    }

    @Override
    public WireNode updateWireNode(WireGraph graph, WireNode node) {
        boolean wasPending = pending;
        if (pending) {
            pending = false;
            if (graph.getLevel().getBlockEntity(getPos()) instanceof WireConnectorBlockEntity be && graph.getLevel().getBlockState(getPos()).getBlock() instanceof IWireConnector) {
                WireNode newNode = null;
                if (be.getNodeId() == null || !graph.hasNode(be.getNodeId().id())) {
                    newNode = graph.createNode(this, new Vector3d(getPos().getX(), getPos().getY(), getPos().getZ()));
                    be.setNodeId(new NodeId(newNode.getId(), graph.getId()));
                    if (ModCommonConfig.WIRE_CONVERTER_LOGGING.get()) PantographsAndWires.LOGGER.info("[GRAPH CONVERTER/UPDATER]        - Create new node: " + newNode.getId());
                } else {
                    newNode = graph.getNode(be.getNodeId().id());
                    if (ModCommonConfig.WIRE_CONVERTER_LOGGING.get()) PantographsAndWires.LOGGER.info("[GRAPH CONVERTER/UPDATER]        - Use existing node: " + newNode.getId());
                }
                return newNode;
            }
        }
        if (ModCommonConfig.WIRE_CONVERTER_LOGGING.get()) PantographsAndWires.LOGGER.info("[GRAPH CONVERTER/UPDATER]        - COULD NOT REPLACE DUMMY NODE! This is not intended, but could occur if there is no longer a valid connector block at the corresponding location.");
        return wasPending ? node : null;
    }
    
    @Override
    public Optional<ConnectorDataProvider> getConnectorCustomData(IWireGraph graph, CustomData customData, int pointIndex) {
        if (!pending && graph.getLevel().isLoaded(getPos()) && SafeChunkUtils.getSafeBE(graph.getLevel(), getPos()) instanceof WireConnectorBlockEntity && SafeChunkUtils.getBlockState(graph.getLevel(), getPos()).getBlock() instanceof IWireConnector connector) {
            return Optional.of(connector.getConnectorData(graph.getLevel(), getPos(), customData, pointIndex));
        }
        return Optional.empty();
    }

    @Override
    public Vector3d toWorldPos(IWireGraph graph) {
        Vector3d v = new Vector3d(getPos().getX(), getPos().getY(), getPos().getZ());
        return v;
    }

    @Override
    public boolean validate(WireGraph graph, WireConnectionDataComponent currentItemData, int pointIndex) {
        return pending || !graph.getLevel().isLoaded(getPos()) || (graph.getLevel().getBlockEntity(getPos()) instanceof WireConnectorBlockEntity);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockConnectorNodeData o) {
            return getPos().equals(o.getPos());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPos());
    }
}
