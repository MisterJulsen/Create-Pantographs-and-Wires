package de.mrjulsen.wires.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class WireNode {

    protected static final String NBT_ID = "Id";
    protected static final String NBT_POS = "Pos";
    protected static final String NBT_DATA = "NodeData";
    protected static final String NBT_CONNECTIONS = "Connections";

    private IWireGraph graph;
    private final UUID id;
    private final NodeData data;
    private Vector3d pos;
    private Set<UUID> connectedWires = new HashSet<>();

    public WireNode(WireGraph graph, NodeData data) {
        this(graph, data, graph.createNewNodeId());
    }

    private WireNode(IWireGraph graph, NodeData data, UUID id) {
        this.graph = graph;
        this.data = data;
        this.id = id;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_ID, id);
        nbt.put(NBT_DATA, data.getRegistryType().wrap(data));
        Utils.putNbtVector3d(nbt, NBT_POS, pos);
        ListTag connectionsList = new ListTag();
        for (UUID cId : connectedWires) {
            connectionsList.add(NbtUtils.createUUID(cId));
        }
        nbt.put(NBT_CONNECTIONS, connectionsList);
        return nbt;
    }

    public static Optional<WireNode> fromNbt(IWireGraph graph, CompoundTag nbt) {
        try {
            List<UUID> connections = nbt.getList(NBT_CONNECTIONS, Tag.TAG_INT_ARRAY).stream().map(x -> NbtUtils.loadUUID(x)).toList();
            WireNode node = new WireNode(graph, WiresApi.NODE_DATA_REGISTRY.load(nbt.getCompound(NBT_DATA)), nbt.getUUID(NBT_ID)); // TODO
            node.pos = Utils.getNbtVector3d(nbt, NBT_POS); // TODO
            node.connectedWires.addAll(connections);
            return Optional.of(node);
        } catch (Exception e) {
            WiresApi.LOGGER.error("Could not load wire node, because the nbt data is invalid: " + nbt, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WireNode o) {
            return id.equals(o.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public IWireGraph getGraph() {
        return graph;
    }

    public void setGraph(IWireGraph graph) {
        this.graph = graph;
    }

    public UUID getId() {
        return id;
    }

    public Vector3d getPos() {
        return new Vector3d(pos);
    }

    public void setPos(Vector3d pos) {
        this.pos = new Vector3d(pos);
    }

    public void addConnection(UUID id) {
        this.connectedWires.add(id);
    }

    public boolean removeConnection(UUID id) {
        this.connectedWires.remove(id);
        return !connectedWires.isEmpty();
    }

    public void onRemove(Level level, Vector3d breakPosition, Optional<Player> player) {
        getData().onRemove(level, breakPosition, player);
    }

    public NodeData getData() {
        return data;
    }

    public Collection<UUID> getConnections() {
        return Collections.synchronizedCollection(connectedWires);
    }
    
}
