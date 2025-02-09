package de.mrjulsen.wires.network;

import java.util.Collection;

import javax.annotation.Nullable;

import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

public record WiresNetworkSyncData(
    @Nullable ChunkPos pos,
    Collection<WireSyncDataEntry> syncData
) {

    private static final String NBT_CHUNK = "Chunk";
    private static final String NBT_SYNC_DATA = "SyncData";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        if (pos != null) Utils.putNbtChunkPos(nbt, NBT_CHUNK, pos);
        ListTag list = new ListTag();
        list.addAll(syncData.stream().map(x -> x.toNbt()).toList());
        nbt.put(NBT_SYNC_DATA, list);
        return nbt;
    }

    public static WiresNetworkSyncData fromNbt(CompoundTag nbt) {
        return new WiresNetworkSyncData(
            nbt.contains(NBT_CHUNK) ? Utils.getNbtChunkPos(nbt, NBT_CHUNK) : null,
            nbt.getList(NBT_SYNC_DATA, Tag.TAG_COMPOUND).stream().map(x -> WireSyncDataEntry.fromNbt((CompoundTag)x)).toList()    
        );
    }

    public static record WireSyncDataEntry(
        WireConnectionSyncData data,
        boolean forceUpdate
    ) {

        private static final String NBT_DATA = "Data";
        private static final String NBT_FORCE_UPDATE = "ForceUpdate";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.put(NBT_DATA, data.toNbt());
            nbt.putBoolean(NBT_FORCE_UPDATE, forceUpdate);
            return nbt;
        }

        public static WireSyncDataEntry fromNbt(CompoundTag nbt) {
            return new WireSyncDataEntry(
                WireConnectionSyncData.fromNbt(nbt.getCompound(NBT_DATA)),
                nbt.getBoolean(NBT_FORCE_UPDATE)
            );
        }
    }
}
