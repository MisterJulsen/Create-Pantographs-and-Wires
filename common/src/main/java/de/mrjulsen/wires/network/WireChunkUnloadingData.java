package de.mrjulsen.wires.network;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import de.mrjulsen.wires.util.GraphId;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

public record WireChunkUnloadingData(
    GraphId id,
    ChunkPos pos,
    Collection<UUID> connections
) {

    private static final String NBT_GRAPH_ID = "GraphId";
    private static final String NBT_CHUNK = "Chunk";
    private static final String NBT_ID = "Id";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag ids = new ListTag();
        ids.addAll(connections.stream().map(x -> StringTag.valueOf(x.toString())).toList());
        nbt.putString(NBT_GRAPH_ID, id.id());
        nbt.put(NBT_ID, ids);
        Utils.putNbtChunkPos(nbt, NBT_CHUNK, pos);
        return nbt;
    }

    public static WireChunkUnloadingData fromNbt(CompoundTag nbt) {
        return new WireChunkUnloadingData(
            new GraphId(nbt.getString(NBT_GRAPH_ID)),
            Utils.getNbtChunkPos(nbt, NBT_CHUNK),
            nbt.getList(NBT_ID, Tag.TAG_STRING).stream().map(x -> UUID.fromString(x.getAsString())).collect(Collectors.toSet())
        );
    }
    
}
