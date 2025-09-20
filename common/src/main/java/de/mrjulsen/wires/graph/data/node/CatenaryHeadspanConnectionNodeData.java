package de.mrjulsen.wires.graph.data.node;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.block.RegistrationArmBlock.State;
import de.mrjulsen.paw.item.CatenaryHeadspanWireItem;
import de.mrjulsen.paw.item.CatenaryHeadspanWireType;
import de.mrjulsen.paw.registry.InsulatorWireDecoration;
import de.mrjulsen.paw.registry.RegistrationArmWireDecoration;
import de.mrjulsen.paw.util.ModMath;
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

public class CatenaryHeadspanConnectionNodeData extends NodeData implements INodeDataWire {

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

    public WireId getWireId() {
        return wireId;
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

        WireNode node = WiresApi.CATENARY_HEADSPAN.getAccessor(graph).map(x -> x.get(wireId).stream().findFirst().orElse(null)).orElseGet(() -> {
            WireEdge edge = graph.getEdge(wireId.id());
            NewWireCollision collision = graph.getCollisionById(wireId.id()).orElse(null);
            if (edge == null || collision == null) {
                return null;
            }
            WirePoints points = collision.getWirePointsOf(wireId.name());
            return graph.createNode(this, points.vertices()[0]);
        });
        node.addConnection(wireId.id());
        return node;
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
        if (points == null) {
            return;
        }
        node.setPos(points.vertices()[0]);
    }

    @Override
    public Optional<ConnectorDataProvider> getConnectorCustomData(WireGraph graph, CustomData customData, WireNode node, int pointIndex) {
        if (wireId == null) {
            return Optional.empty();
        }
        WireEdge edge = graph.getEdge(wireId.id());
        NewWireCollision collision = graph.getCollisionById(wireId.id()).orElse(null);
        Optional<UUID> dropperIdOpt = CatenaryHeadspanWireType.toDropperId(wireId.name());
        if (edge == null || collision == null || dropperIdOpt.isEmpty()) {
            return Optional.empty();
        }
        
        WirePoints dropperWirePoints = collision.getWirePointsOf(wireId.name());
        WirePoints tensionWirePoints = collision.getWirePointsOf(CatenaryHeadspanWireType.WIRE_LOWER_TENSION);
        if (dropperWirePoints == null || tensionWirePoints == null) {
            return Optional.empty();
        }
        boolean b = edge.getWireConnectionData().customData().getCommonData().getFloat(CatenaryHeadspanWireItem.NBT_UPPER_WIRE_HEIGHT) > 1.5f;
        
        Optional<RegistrationArmWireDecoration> registrationArm = CatenaryHeadspanWireType.getRegistrationArmForDropper(edge, dropperIdOpt.get());
        Vector3f headspanDirection = new Vector3f(tensionWirePoints.vertices()[tensionWirePoints.vertices().length - 1]).sub(tensionWirePoints.vertices()[0]).normalize();
        Vector3f offset = new Vector3f(registrationArm.map(x -> (x.getVariant() == State.NORMAL_CENTERED || x.getVariant() == State.ABOVE_CENTERED) ? 0 : (x.isMirrored() ? 0.25f : -0.25f)).orElse(0f), 0, 0);
        Vector3f offsetVec = ModMath.rotateToDirection(offset, headspanDirection);

        return Optional.of(new CantileverConnectorDataProvider(
            new Vector3f(offsetVec.x(), registrationArm.map(x -> x.getVariant().isAbove() ? DragonLib.PIXEL * 4 : DragonLib.PIXEL * -6).orElse(0f), offsetVec.z()),
            new Vector3f(dropperWirePoints.vertices()[dropperWirePoints.vertices().length - 1]).sub(node.getPos()).add(0, b ? -InsulatorWireDecoration.RADIUS * 2 : 0, 0)
        ));
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
        return graph.getCollisionById(wireId.id()).map(x -> x.hasWire(wireId.name())).orElse(false);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CatenaryHeadspanConnectionNodeData o) {
            return wireId.equals(o.wireId);
        }
        return false;
    }
}
