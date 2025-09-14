package de.mrjulsen.wires.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.joml.Vector3f;

import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WireTypeRegistry;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.decoration.WireDecorationData;
import de.mrjulsen.wires.decoration.WireDecorationElement;
import de.mrjulsen.wires.graph.data.WireConnectionData;
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
    private final UUID nodeA;
    private final UUID nodeB;
    private final Map<String, TreeMap<Float, WireDecorationData>> decorations = new HashMap<>();

    public WireEdge(WireGraph graph, IWireType type, WireConnectionData customData, UUID nodeA, UUID nodeB) {
        this(graph, graph.createNewEdgeId(), type, customData, nodeA, nodeB);
    }
    
    private WireEdge(IWireGraph graph, UUID id, IWireType type, WireConnectionData customData, UUID nodeA, UUID nodeB) {
        this.graph = graph;
        this.id = id;
        this.wireType = type;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.customData = customData;
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
        try {
            WireEdge edge = new WireEdge(
                graph,
                nbt.getUUID(NBT_ID),
                WireTypeRegistry.get(new ResourceLocation(nbt.getString(NBT_WIRE_TYPE))),
                WireConnectionData.fromNbt(nbt.getCompound(NBT_CUSTOM_DATA)),
                nbt.getUUID(NBT_NODE_A),
                nbt.getUUID(NBT_NODE_B)
            );
            
            nbt.getList(NBT_DECORATIONS, Tag.TAG_COMPOUND).forEach(x -> {
                edge.addDecoration(WireDecorationData.fromNbt((CompoundTag)x));
            });

            //if (!graph.hasNode(edge.getNodeAId()) || !graph.hasNode(edge.getNodeBId())) {
            //    //throw new IllegalStateException("One of the wire connection nodes no longer exists.");
            //}
            return Optional.of(edge);
        } catch (Exception e) {
            WiresApi.LOGGER.error("Could not load wire connection, because the nbt data is invalid: " + nbt, e);
        }
        return Optional.empty();
    }

    public boolean addDecoration(Vector3f pos, String wireName, WireDecorationElement<?> element) {
        if (!(getGraph() instanceof WireGraph graph)) {
            return false;
        }

        float d = graph.getCollisionById(id).map(x -> x.worldPosToWirePos(wireName, pos)).orElse(0F);
        if (decorations.containsKey(wireName)) {
            TreeMap<Float, WireDecorationData> map = decorations.get(wireName);
            Map.Entry<Float, WireDecorationData> lower = map.lowerEntry(d);
            Map.Entry<Float, WireDecorationData> upper = map.ceilingEntry(d);
            if ((lower != null && lower.getKey() + lower.getValue().getDecoration().getRadius() > d - element.getRadius()) ||
                (upper != null && upper.getKey() - upper.getValue().getDecoration().getRadius() < d + element.getRadius())) {
                    return false;
            }
        }
        WireDecorationData decoration = new WireDecorationData(wireName, d, element);
        addDecoration(decoration);
        graph.sendEdgeToClient(this);
        return true;
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
            if (decoration.getPos() + decoration.getDecoration().getRadius() >= d && decoration.getPos() - decoration.getDecoration().getRadius() <= d) {
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
        Collection<WireDecorationData> decorations = new ArrayList<>();
        for (TreeMap<Float, WireDecorationData> decor : this.decorations.values()) {
            for (WireDecorationData d : decor.values()) {
                decorations.add(d);
            }
        }
        return decorations;
    }

    

    public void onRemove(Level level, Vector3f breakPosition, Optional<Player> player) {           
        if (getGraph() instanceof WireGraph graph) {
            for (TreeMap<Float, WireDecorationData> e : decorations.values()) {
                for (WireDecorationData decoration : e.values()) {
                    decoration.getDecoration().onBreak(level, graph.getCollisionById(id).map(x -> x.wirePosToWorldPos(decoration.getWireName(), decoration.getPos())).orElse(new Vector3f()), player);
                }
            }
        }
        
        wireType.onBreak(level, breakPosition, player);
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
