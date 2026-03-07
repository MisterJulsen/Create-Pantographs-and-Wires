package de.mrjulsen.wires.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import org.joml.Vector3f;

import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WireTypeRegistry;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.decoration.WireDecorationData;
import de.mrjulsen.wires.decoration.IWireDecoration;
import de.mrjulsen.wires.graph.data.WireEdgeHash;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.accessor.GenericWireNodeAccessor;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class WireEdge {

    protected static final String NBT_ID = "Id";
    protected static final String NBT_WIRE_TYPE = "WireType";
    protected static final String NBT_CUSTOM_DATA = "EdgeData";
    protected static final String NBT_NODE_A = "NodeA";
    protected static final String NBT_NODE_B = "NodeB";
    protected static final String NBT_DECORATIONS = "Decorations";

    private final IWireGraph graph;
    private final UUID id;
    private WireConnectionData customData;
    
    private final IWireType wireType;
    private UUID nodeA;
    private UUID nodeB;
    private final WireEdgeHash hash;
    private final Map<String, TreeMap<Float, WireDecorationData>> decorations = new HashMap<>();

    public WireEdge(WireGraph graph, IWireType type, WireConnectionData customData, UUID nodeA, UUID nodeB, WireEdgeHash hash) {
        this(graph, graph.createNewEdgeId(), type, customData, nodeA, nodeB, hash);
    }
    
    private WireEdge(IWireGraph graph, UUID id, IWireType type, WireConnectionData customData, UUID nodeA, UUID nodeB, WireEdgeHash hash) {
        this.graph = graph;
        this.id = id;
        this.wireType = type;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.customData = customData;
        this.hash = hash;
    }

    void swapNodes(UUID nodeA, UUID nodeB) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }

    public WireEdgeHash getHash() {
        return hash;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_WIRE_TYPE, wireType.getRegistryId().toString());
        nbt.putUUID(NBT_ID, id);
        nbt.putUUID(NBT_NODE_A, nodeA);
        nbt.putUUID(NBT_NODE_B, nodeB);
        nbt.put(NBT_CUSTOM_DATA, customData.toNbt());
        ListTag decorationsList = new ListTag();
        for (WireDecorationData deco : getDecorations()) {
            decorationsList.add(deco.toNbt());
        }
        nbt.put(NBT_DECORATIONS, decorationsList);
        return nbt;
    }

    public static Optional<WireEdge> fromNbt(IWireGraph graph, CompoundTag nbt) {
        boolean isClient = graph instanceof WireGraphClient;
        try {
            UUID nodeAId = nbt.getUUID(NBT_NODE_A);
            UUID nodeBId = nbt.getUUID(NBT_NODE_B);
            if (!graph.hasNode(nodeAId) || !graph.hasNode(nodeBId)) {
                throw new IllegalStateException("One of the wire connection nodes no longer exists.");
            }
            
            WireConnectionData customData = WireConnectionData.fromNbt(nbt.getCompound(NBT_CUSTOM_DATA));
            WireEdgeHash hash = new WireEdgeHash(customData.customData(), graph.getNode(nodeAId), graph.getNode(nodeBId));
            if (!isClient && ((WireGraph)graph).hasEdge(hash)) {
                throw new IllegalStateException("An equivalent edge with the same data already exists. This edge is skipped.");
            }
            WireEdge edge = new WireEdge(
                graph,
                nbt.getUUID(NBT_ID),
                WireTypeRegistry.get(DLUtils.resourceLocation(nbt.getString(NBT_WIRE_TYPE))),
                customData,
                nodeAId,
                nodeBId,
                hash
            );
            
            nbt.getList(NBT_DECORATIONS, Tag.TAG_COMPOUND).forEach(x -> {
                edge.addDecoration(WireDecorationData.fromNbt((CompoundTag)x));
            });
            return Optional.of(edge);
        } catch (Exception e) {
            WiresApi.LOGGER.error("Could not load wire connection, because the nbt data is invalid: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean addDecoration(Vector3f pos, String wireName, IWireDecoration<?> element) {
        if (!(getGraph() instanceof WireGraph graph)) {
            return false;
        }

        float d = graph.getCollisionById(id).map(x -> x.worldPosToWirePos(wireName, pos)).orElse(0F);
        return addDecoration(d, wireName, element);
    }

    public boolean addDecoration(float posOnWire, String wireName, IWireDecoration<?> element) {
        if (!(getGraph() instanceof WireGraph graph)) {
            return false;
        }

        if (!canPlaceDecoration(posOnWire, wireName, element)) {
            return false;
        }
        
        WireDecorationData decoration = new WireDecorationData(wireName, posOnWire, element);
        addDecoration(decoration);
        graph.sendEdgeToClient(this, true);
        return true;
    }

    public boolean canPlaceDecoration(float posOnWire, String wireName, IWireDecoration<?> element) {
        return !isOccupied(posOnWire, wireName, element.getRadius(element));
    }
    
    public boolean isOccupied(float posOnWire, String wireName, float radius) {
        if (decorations.containsKey(wireName)) {
            TreeMap<Float, WireDecorationData> map = decorations.get(wireName);
            Map.Entry<Float, WireDecorationData> lower = map.lowerEntry(posOnWire);
            Map.Entry<Float, WireDecorationData> upper = map.ceilingEntry(posOnWire);
            if ((lower != null && lower.getKey() + lower.getValue().getDecoration().getRadius(null) > posOnWire - radius) ||
                (upper != null && upper.getKey() - upper.getValue().getDecoration().getRadius(null) < posOnWire + radius)) {
                    return true;
            }
        }
        return false;
    }

    private void addDecoration(WireDecorationData decoration) {
        this.decorations.computeIfAbsent(decoration.getWireName(), x -> new TreeMap<>()).put(decoration.getPos(), decoration);

        if (getGraph() instanceof WireGraph graph) {
            graph.setDirty();
        }
    }

    public List<WireDecorationData> getDecorationsAt(Vector3f pos, String wireName) {        
        if (!(getGraph() instanceof WireGraph graph)) {
            return List.of();
        }


        float d = graph.getCollisionById(id).map(x -> x.worldPosToWirePos(wireName, pos)).orElse(0F);
        if (!decorations.containsKey(wireName)) {
            return List.of();
        }
        TreeMap<Float, WireDecorationData> map = decorations.get(wireName);
        List<WireDecorationData> decoResult = new ArrayList<>(2);
        for (WireDecorationData decoration : map.values()) {
            if (decoration.getPos() + decoration.getDecoration().getRadius(null) >= d && decoration.getPos() - decoration.getDecoration().getRadius(null) <= d) {
                decoResult.add(decoration);
            }
        }
        return decoResult;
    }

    public void removeDecorations(Level level, Optional<Player> player, String wireName, List<WireDecorationData> decorations) {      
        if (!(getGraph() instanceof WireGraph graph)) {
            return;
        }

        if (this.decorations.containsKey(wireName)) {
            TreeMap<Float, WireDecorationData> map = this.decorations.get(wireName);
            map.values().removeAll(decorations);
            for (WireDecorationData deco : decorations) {
                deco.getDecoration().onBreak(level, graph.getCollisionById(id).map(x -> x.wirePosToWorldPos(wireName, deco.getPos())).orElse(new Vector3f()), player);
            }
            if (map.isEmpty()) {
                this.decorations.remove(wireName);
            }
        }
        graph.setDirty();
    }    

    public Collection<WireDecorationData> getDecorations() {
        return getDecorations((d) -> true);
    }

    public Collection<WireDecorationData> getDecorations(Predicate<WireDecorationData> condition) {
        Collection<WireDecorationData> decorations = new ArrayList<>();
        for (TreeMap<Float, WireDecorationData> decor : this.decorations.values()) {
            for (WireDecorationData d : decor.values()) {
                if (condition.test(d)) {
                    decorations.add(d);
                }
            }
        }
        return decorations;
    }

    public void queryDecorations(Predicate<WireDecorationData> condition, Consumer<WireDecorationData> callback) {
        for (TreeMap<Float, WireDecorationData> decor : this.decorations.values()) {
            for (WireDecorationData d : decor.values()) {
                if (condition.test(d)) {
                    callback.accept(d);
                }
            }
        }
    }

    

    public void onRemove(Level level, Vector3f breakPosition, Optional<Player> player) {           
        if (getGraph() instanceof WireGraph graph) {
            for (TreeMap<Float, WireDecorationData> e : decorations.values()) {
                for (WireDecorationData decoration : e.values()) {
                    decoration.getDecoration().onBreak(level, graph.getCollisionById(id).map(x -> x.wirePosToWorldPos(decoration.getWireName(), decoration.getPos())).orElse(breakPosition), player);
                }
            }

            for (NodeDataRegistryObject<? extends NodeData, ? extends NodeAccessor<? extends NodeData>> type : WiresApi.NODE_DATA_REGISTRY.getRegisteredTypes()) {
                Optional<? extends NodeAccessor<?>> accessor = type.getAccessor(graph);
                if (accessor.isPresent() && accessor.get() instanceof GenericWireNodeAccessor a) {
                    Collection<WireNode> nodes = new ArrayList<>(a.get(id));
                    for (WireNode node : nodes) {
                        graph.removeNode(node.getId(), breakPosition, player);
                        a.remove(node);
                    }
                }
            }
        }
        
        wireType.onBreak(level, breakPosition, player, getGraph(), this);
        
    }

    public Vector3f getCenterPos() {
        Collection<Vector3f> vectors = List.of(graph.getNode(getNodeAId()).getPos(), graph.getNode(getNodeBId()).getPos());
        if (vectors == null || vectors.isEmpty()) {
            return null;
        }

        Vector3f summe = new Vector3f(0, 0, 0);
        for (Vector3f v : vectors) {
            summe.add(v);
        }

        return summe.div(vectors.size());
    }
    

    public int length() {
        return (int)graph.getNode(getNodeAId()).getPos().distance(graph.getNode(getNodeBId()).getPos());
    }



    public IWireGraph getGraph() {
        return graph;
    }

    public UUID getId() {
        return id;
    }

    public IWireType getType() {
        return wireType;
    }

    public UUID getNodeAId() {
        return nodeA;
    }

    public UUID getNodeBId() {
        return nodeB;
    }

    public WireConnectionData getWireConnectionData() {
        return customData;
    }

    public void setWireConnectionData(WireConnectionData data) {
        this.customData = data;
    }
}
