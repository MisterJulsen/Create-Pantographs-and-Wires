package de.mrjulsen.wires.graph.data.node;

import java.util.*;
import java.util.stream.Collectors;

import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.DataCache;
import net.minecraft.nbt.Tag;
import org.joml.Vector3d;
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
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.NewWireCollision;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.item.CustomData;
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
        return node;
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
        WirePoints points = collision.getWirePointsOf(wireId.name());
        if (points == null) {
            return null;
        }
        node.setPos(points.vertices()[0]);
        return node;
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
        if (!registrationArm.isPresent()) {
            return Optional.empty();
        }

        Vector3d vert1 = tensionWirePoints.vertices()[tensionWirePoints.vertices().length - 1];
        Vector3d vert2 = tensionWirePoints.vertices()[0];
        Vector3f headspanDirection = new Vector3f((float)vert1.x(), (float)vert1.y(), (float)vert1.z()).sub(new Vector3f((float)vert2.x(), (float)vert2.y(), (float)vert2.z())).normalize();
        Vector3f offset = new Vector3f(registrationArm.map(x -> (x.getVariant() == State.NORMAL_CENTERED || x.getVariant() == State.ABOVE_CENTERED) ? 0 : (x.isMirrored() ? 0.25f : -0.25f)).orElse(0f), 0, 0);
        Vector3f offsetVec = ModMath.rotateToDirection(offset, headspanDirection);

        return Optional.of(new CantileverConnectorDataProvider(
            new Vector3d(offsetVec.x(), registrationArm.map(x -> x.getVariant().isAbove() ? DragonLib.BLOCK_PIXEL * 4 : DragonLib.BLOCK_PIXEL * -6).orElse(0f), offsetVec.z()),
            new Vector3d(dropperWirePoints.vertices()[dropperWirePoints.vertices().length - 1]).sub(node.getPos()).add(0, b ? -InsulatorWireDecoration.RADIUS * 2 : 0, 0)
        ));
    }       

    @Override
    public Vector3d toWorldPos(IWireGraph graph) {
        if (wireId == null) {
            return new Vector3d();
        }
        WireEdge edge = graph.getEdge(wireId.id());

        NewWireCollision collision = null;
        if (graph instanceof WireGraph g) {
            collision = g.getCollisionById(wireId.id()).orElse(null);
        } else if (graph instanceof WireGraphClient gc) {
            collision = gc.getCollisionById (wireId.id()).orElse(null);
        }

        if (edge == null || collision == null) {
            return new Vector3d();
        }

        Map<UUID, CatenaryHeadspanWireType.Dropper> droppers = edge.getWireConnectionData().customData().getCommonData().getList(CatenaryHeadspanWireType.NBT_DROPPERS, Tag.TAG_COMPOUND).stream().map(x -> CatenaryHeadspanWireType.Dropper.fromNbt((CompoundTag)x)).collect(Collectors.toMap(x -> x.id(), x -> x));
        WireNode a = graph.getNode(edge.getNodeAId());
        WireNode b = graph.getNode(edge.getNodeBId());
        Vector3d dir = new Vector3d(b.getPos()).sub(a.getPos()).mul(droppers.get(CatenaryHeadspanWireType.toDropperId(wireId.name()).orElse(null)).pos());
        Vector3d p = new Vector3d(a.getPos()).add(dir);
        //Vector3d pos = collision.getWirePointsOf(wireId.name()).vertices()[0];
        return p;
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

    @Override
    public int hashCode() {
        return Objects.hash(wireId);
    }
}
