package de.mrjulsen.wires.network;

import java.util.UUID;

import de.mrjulsen.wires.network.WiresNetworkSyncData.WireSyncDataEntry;
import de.mrjulsen.wires.util.ClientUtils;
import de.mrjulsen.wires.WireClientNetwork;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessorType;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class NetworkManager {

    public static void init() {}

    public static final DataAccessorType<WiresNetworkSyncData, Void, Void> WIRE_CONNECTOR_DATA_TRANSFER = DataAccessorType.register(new ResourceLocation(WiresApi.MOD_ID, "wire_connection_data_transfer"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> { 
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return WiresNetworkSyncData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            for (WireSyncDataEntry syncData : in.syncData()) {
                WireClientNetwork.get(ClientUtils.level()).createClientConnection(in.pos(), syncData);
            }
            return false;
        }
    ));
    
    public static final DataAccessorType<UUID[], Void, Void> DELETE_WIRE_CONNECTION = DataAccessorType.register(new ResourceLocation(WiresApi.MOD_ID, "delete_wire_conection"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            ListTag list = new ListTag();
            for (int i = 0; i < in.length; i++) {
                UUID id = in[i];
                list.add(StringTag.valueOf(id.toString()));
            }
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, list);
        }, (nbt) -> {
            return nbt.getList(DataAccessorType.DEFAULT_NBT_DATA, Tag.TAG_STRING).stream().map(x -> UUID.fromString(((StringTag)x).getAsString())).toArray(UUID[]::new);
        }, (player, in, temp, nbt, iteration) -> {
            WireClientNetwork.get(ClientUtils.level()).removeClientConnections(in);
            return false;
        }
    ));
    
    public static final DataAccessorType<WireChunkLoadingData, Void, Void> WIRE_CONNECTION_CHUNK_LOADING = DataAccessorType.register(new ResourceLocation(WiresApi.MOD_ID, "wire_connection_chunk_loading"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return WireChunkLoadingData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            WireClientNetwork.get(ClientUtils.level()).onClientChunkLoading(in);
            return false;
        }
    ));    
}
