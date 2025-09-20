package de.mrjulsen.wires.graph.data.node;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.item.WireBaseItem.CustomData;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class GenericBlockNodeData extends NodeData implements INodeDataBlock {

    private static final String NBT_POS = "Pos";
    private static final String NBT_ATTACH_POINT = "AttachPoint";
    private BlockPos pos;
    private Vector3f attachPoint;

    public GenericBlockNodeData() {}

    public GenericBlockNodeData(BlockPos pos, Vector3f attachPoint) {
        this.pos = pos;
        this.attachPoint = attachPoint;
    }

    @Override
    public BlockPos getBlockPos() {        
        return pos;
    }

    public Vector3f getAttachPoint() {
        return attachPoint;
    }
    
    @Override
    public NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> getRegistryType() {
        return (NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>>)(Object)WiresApi.GENERIC_BLOCK;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        Utils.putNbtBlockPos(nbt, NBT_POS, pos);
        Utils.putNbtVector3f(nbt, NBT_ATTACH_POINT, attachPoint);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        this.pos = Utils.getNbtBlockPos(nbt, NBT_POS);
        this.attachPoint = Utils.getNbtVector3f(nbt, NBT_ATTACH_POINT);
    }
    
    @Override
    public WireNode getOrCreateNode(WireGraph graph) {
        return graph.createNode(this, new Vector3f(attachPoint.x(), attachPoint.y(), attachPoint.z()));
    }
    
    @Override
    public Optional<ConnectorDataProvider> getConnectorCustomData(WireGraph graph, CustomData customData, WireNode node, int pointIndex) {
        return Optional.of(new BasicConnectorDataProvider(new Vector3f()));
    }

    @Override
    public Vector3f toWorldPos(WireGraph graph) {
        return new Vector3f(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()).add(getAttachPoint());
    }

    @Override
    public boolean validate(WireGraph graph, CompoundTag currentItemData, int pointIndex) {
        return graph.getLevel().isLoaded(pos) && !graph.getLevel().getBlockState(pos).isAir();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenericBlockNodeData o) {
            return getBlockPos().equals(o.getBlockPos()) && getAttachPoint().distance(o.getAttachPoint()) > DragonLib.PIXEL * 4;
        }
        return false;
    }
    
}
