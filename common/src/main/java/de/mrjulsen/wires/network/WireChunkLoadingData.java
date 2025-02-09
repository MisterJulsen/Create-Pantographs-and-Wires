package de.mrjulsen.wires.network;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

public record WireChunkLoadingData(
    ChunkPos pos,
    Set<UUID> connections,
    boolean load
) {

    private static final String NBT_CHUNK = "Chunk";
    private static final String NBT_ID = "Id";
    private static final String NBT_LOAD = "Load";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag ids = new ListTag();
        ids.addAll(connections.stream().map(x -> StringTag.valueOf(x.toString())).toList());
        nbt.put(NBT_ID, ids);
        nbt.putBoolean(NBT_LOAD, load);
        Utils.putNbtChunkPos(nbt, NBT_CHUNK, pos);
        return nbt;
    }

    public static WireChunkLoadingData fromNbt(CompoundTag nbt) {
        return new WireChunkLoadingData(
            Utils.getNbtChunkPos(nbt, NBT_CHUNK),
            nbt.getList(NBT_ID, Tag.TAG_STRING).stream().map(x -> UUID.fromString(x.getAsString())).collect(Collectors.toSet()),
            nbt.getBoolean(NBT_LOAD)
        );
    }
    
}
