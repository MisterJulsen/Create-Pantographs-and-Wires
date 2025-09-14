package de.mrjulsen.wires.util;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;

public record EdgeId(UUID id, GraphId graphId) {

    private static final String NBT_ID = "Id";
    private static final String NBT_GRAPH_ID = "GraphId";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_ID, id);
        nbt.putString(NBT_GRAPH_ID, graphId.id());
        return nbt;
    }

    public static EdgeId fromNbt(CompoundTag nbt) {
        return new EdgeId(
            nbt.getUUID(NBT_ID),
            new GraphId(nbt.getString(NBT_GRAPH_ID))
        );
    }
}
