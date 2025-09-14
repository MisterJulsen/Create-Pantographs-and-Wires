package de.mrjulsen.wires.network;

import java.util.List;
import java.util.UUID;

import de.mrjulsen.wires.util.GraphId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public record DeleteWireSyncData(
    GraphId id,
    List<UUID> wireEdgeIds
) {

    private static final String NBT_GRAPH_ID = "GraphId";
    private static final String NBT_WIRE_EDGE_IDS = "WireEdgeIDs";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();            
        ListTag list = new ListTag();
        for (int i = 0; i < wireEdgeIds.size(); i++) {
            UUID id = wireEdgeIds.get(i);
            list.add(StringTag.valueOf(id.toString()));
        }
        nbt.put(NBT_WIRE_EDGE_IDS, list);
        nbt.putString(NBT_GRAPH_ID, id.id());
        return nbt;
    }

    public static DeleteWireSyncData fromNbt(CompoundTag nbt) {
        return new DeleteWireSyncData(
            new GraphId(nbt.getString(NBT_GRAPH_ID)),
            nbt.getList(NBT_WIRE_EDGE_IDS, Tag.TAG_STRING).stream().map(x -> UUID.fromString(((StringTag)x).getAsString())).toList()
        );
    }
}
