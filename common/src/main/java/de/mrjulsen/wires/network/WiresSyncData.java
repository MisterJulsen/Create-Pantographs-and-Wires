package de.mrjulsen.wires.network;

import java.util.Collection;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.util.GraphId;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record WiresSyncData(
    GraphId id,
    @Nullable ChunkPos pos,
    Supplier<Collection<WireEdge>> edges,
    Supplier<Collection<WireNode>> nodes,
    boolean forceUpdate
) {

    private static final String NBT_GRAPH_ID = "GraphId";
    private static final String NBT_CHUNK = "Chunk";
    private static final String NBT_NODES = "Nodes";
    private static final String NBT_EDGES = "Edges";
    private static final String NBT_FORCE_UPDATE = "ForceUpdate";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (pos != null) Utils.putNbtChunkPos(nbt, NBT_CHUNK, pos);
        ListTag edgesList = new ListTag();
        for (WireEdge edge : edges.get()) {
            edgesList.add(edge.toNbt());
        }
        ListTag nodesList = new ListTag();
        for (WireNode node : nodes.get()) {
            if (node == null) continue;
            nodesList.add(node.toNbt());
        }
        nbt.putString(NBT_GRAPH_ID, id.id());
        nbt.put(NBT_EDGES, edgesList);
        nbt.put(NBT_NODES, nodesList);
        nbt.putBoolean(NBT_FORCE_UPDATE, forceUpdate);
        return nbt;
    }

    public static WiresSyncData fromNbt(Level level, CompoundTag nbt) {
        GraphId id = new GraphId(nbt.getString(NBT_GRAPH_ID));
        WireGraphClient graph = WireGraphManager.getClient(level, id);
        return new WiresSyncData(
            id,
            nbt.contains(NBT_CHUNK) ? Utils.getNbtChunkPos(nbt, NBT_CHUNK) : null,
            () -> nbt.getList(NBT_EDGES, Tag.TAG_COMPOUND).stream().map(x -> WireEdge.fromNbt(graph, (CompoundTag)x).orElse(null)).toList(),
            () -> nbt.getList(NBT_NODES, Tag.TAG_COMPOUND).stream().map(x -> WireNode.fromNbt(graph, (CompoundTag)x).orElse(null)).toList(),
            nbt.getBoolean(NBT_FORCE_UPDATE)   
        );
    }


    public static class Wrapper {
        private final CompoundTag nbt;

        public Wrapper(WiresSyncData data) {
            this(data.toNbt());
        }

        private Wrapper(CompoundTag nbt) {
            this.nbt = nbt;
        }

        public CompoundTag toNbt() {
            return nbt;
        }

        public static Wrapper fromNbt(CompoundTag nbt) {
            return new Wrapper(nbt);
        }

        public WiresSyncData unwrap(Level level) {
            return WiresSyncData.fromNbt(level, nbt);
        }
    }
}
