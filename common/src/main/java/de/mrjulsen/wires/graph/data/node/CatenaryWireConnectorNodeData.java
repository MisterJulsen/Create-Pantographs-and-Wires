package de.mrjulsen.wires.graph.data.node;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.NewWireCollision;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.item.WireBaseItem.CustomData;
import de.mrjulsen.wires.network.WireId;
import net.minecraft.nbt.CompoundTag;

public class CatenaryWireConnectorNodeData extends NodeData {

    private static final String NBT_WIRE_ID = "WireId";
    private static final String NBT_POS = "PosPercentage";
    private WireId wireId;
    private float posPercentage;

    public CatenaryWireConnectorNodeData() {}

    public CatenaryWireConnectorNodeData(WireId wireId, float posPercentage) {
        this.wireId = wireId;
        this.posPercentage = posPercentage;
    }
    
    @Override
    public NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> getRegistryType() {
        return (NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>>)(Object)WiresApi.WIRE_CONNECTOR;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        if (wireId != null) nbt.put(NBT_WIRE_ID, wireId.toNbt());
        nbt.putFloat(NBT_POS, posPercentage);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        if (nbt.contains(NBT_WIRE_ID)) this.wireId = WireId.fromNbt(nbt.getCompound(NBT_WIRE_ID)).orElse(null);
        this.posPercentage = nbt.getFloat(NBT_POS);
    }
    
    @Override
    public WireNode getOrCreateNode(WireGraph graph) {
        if (wireId == null) {
            return null;
        }

        WireEdge edge = graph.getEdge(wireId.id());
        NewWireCollision collision = graph.getCollisionById(wireId.id()).orElse(null);
        if (edge == null || collision == null) {
            return null;
        }

        float length = collision.length(wireId.name());
        float posOnWire = length * posPercentage;
        Vector3f pos = collision.wirePosToWorldPos(wireId.name(), posOnWire);
        return graph.createNode(this, pos);
    }

    @Override
    public WireNode updateWireNode(WireGraph graph, WireNode node) {
        if (wireId == null) {
            return null;
        }

        WireEdge edge = graph.getEdge(wireId.id());
        NewWireCollision collision = graph.getCollisionById(wireId.id()).orElse(null);
        if (edge == null || collision == null) {
            return null;
        }  
        float length = collision.length(wireId.name());
        float posOnWire = length * posPercentage;
        node.setPos(collision.wirePosToWorldPos(wireId.name(), posOnWire));
        return node;
    }
    
    @Override
    public Optional<ConnectorDataProvider> getConnectorCustomData(WireGraph graph, CustomData customData, WireNode node, int pointIndex) {
        return Optional.of(new CantileverConnectorDataProvider(new Vector3f(0, 0, 0), new Vector3f(0)));
    }       

    @Override
    public Vector3f toWorldPos(WireGraph graph) {
        WireEdge edge = graph.getEdge(wireId.id());
        NewWireCollision collision = graph.getCollisionById(wireId.id()).orElse(null);
        if (edge == null || collision == null) {
            return new Vector3f(0);
        }  
        float length = collision.length(wireId.name());
        float posOnWire = length * posPercentage;
        return collision.wirePosToWorldPos(wireId.name(), posOnWire);
    }

    @Override
    public boolean validate(WireGraph graph, CompoundTag currentItemData, int pointIndex) {
        return graph.hasEdge(wireId.id());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CatenaryWireConnectorNodeData o) {
            return wireId.equals(o.wireId) && posPercentage == o.posPercentage;
        }
        return false;
    }
}
