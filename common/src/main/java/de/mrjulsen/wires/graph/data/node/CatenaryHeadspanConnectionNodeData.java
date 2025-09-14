package de.mrjulsen.wires.graph.data.node;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.wires.WirePoints;
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

public class CatenaryHeadspanConnectionNodeData extends NodeData {

    private static final String NBT_WIRE_ID = "WireId";
    private WireId wireId;

    public CatenaryHeadspanConnectionNodeData() {}

    public CatenaryHeadspanConnectionNodeData(WireId wireId) {
        this.wireId = wireId;
    }
    
    @Override
    public NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> getRegistryType() {
        return (NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>>)(Object)WiresApi.CATENARY_HEADSPAN;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        if (wireId != null) nbt.put(NBT_WIRE_ID, wireId.toNbt());
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        if (nbt.contains(NBT_WIRE_ID)) this.wireId = WireId.fromNbt(nbt.getCompound(NBT_WIRE_ID)).orElse(null);
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
        WirePoints points = collision.getWirePointsOf(wireId.name());
        return graph.createNode(this, points.vertices()[0]);
    }

    @Override
    public void updateWireNode(WireGraph graph, WireNode node) {
        if (wireId == null) {
            return;
        }
        WireEdge edge = graph.getEdge(wireId.id());
        NewWireCollision collision = graph.getCollisionById(wireId.id()).orElse(null);
        if (edge == null || collision == null) {
            return;
        }
        WirePoints points = collision.getWirePointsOf(wireId.name());
        node.setPos(points.vertices()[0]);
    }

    @Override
    public Optional<ConnectorDataProvider> getConnectorCustomData(WireGraph graph, CustomData customData, WireNode node, int pointIndex) {
        if (wireId == null) {
            return Optional.empty();
        }
        WireEdge edge = graph.getEdge(wireId.id());
        NewWireCollision collision = graph.getCollisionById(wireId.id()).orElse(null);
        if (edge == null || collision == null) {
            return Optional.empty();
        }
        WirePoints points = collision.getWirePointsOf(wireId.name());
        return Optional.of(new CantileverConnectorDataProvider(new Vector3f(0, -0.5f, 0), new Vector3f(points.vertices()[points.vertices().length - 1]).sub(node.getPos()).add(0, -0.7f, 0)));
    }       

    @Override
    public Vector3f toWorldPos(WireGraph graph) {
        if (wireId == null) {
            return new Vector3f();
        }
        WireEdge edge = graph.getEdge(wireId.id());
        NewWireCollision collision = graph.getCollisionById(wireId.id()).orElse(null);
        if (edge == null || collision == null) {
            return new Vector3f();
        }
        WireNode originNode = graph.getNode(edge.getNodeAId());
        return originNode.getPos();
    }

    @Override
    public boolean validate(WireGraph graph, CompoundTag currentItemData, int pointIndex) {
        return graph.hasEdge(wireId.id());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CatenaryHeadspanConnectionNodeData o) {
            return wireId.equals(o.wireId);
        }
        return false;
    }
}
