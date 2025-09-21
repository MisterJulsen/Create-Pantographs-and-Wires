package de.mrjulsen.wires.network;

import java.util.UUID;

import de.mrjulsen.wires.util.ClientUtils;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.item.IWireInteractableItem;
import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessorType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

public final class NetworkManager {

    public static void init() {}

    public static final DataAccessorType<WiresSyncData.Wrapper, Void, Void> WIRE_CONNECTOR_DATA_TRANSFER = DataAccessorType.register(new ResourceLocation(WiresApi.MOD_ID, "wire_connection_data_transfer"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> { 
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return WiresSyncData.Wrapper.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            WiresSyncData data = in.unwrap(player.level());
            WireGraphClient graph = WireGraphManager.getClient(player.level(), data.id());
            for (WireNode node : data.nodes()) {
                graph.addNode(node);
            }
            for (WireEdge edge : data.edges()) {
                graph.addEdge(edge);
            }
            return false;
        }
    ));
    
    public static final DataAccessorType<DeleteWireSyncData, Void, Void> DELETE_WIRE_CONNECTION = DataAccessorType.register(new ResourceLocation(WiresApi.MOD_ID, "delete_wire_conection"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return DeleteWireSyncData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            WireGraphClient graph = WireGraphManager.getClient(player.level(), in.id());
            for (UUID id : in.wireEdgeIds()) {
                graph.removeEdge(id);
            }
            return false;
        }
    ));
    
    public static final DataAccessorType<WireChunkLoadingData, Void, Void> WIRE_CONNECTION_CHUNK_LOADING = DataAccessorType.register(new ResourceLocation(WiresApi.MOD_ID, "wire_connection_chunk_loading"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return WireChunkLoadingData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA));
        }, (player, in, temp, nbt, iteration) -> {
            WireGraphManager.getClient(ClientUtils.level(), in.id()).onClientChunkLoading(in);
            return false;
        }
    ));
    
    public static final DataAccessorType<WireInteractionData, Void, Void> WIRE_INTERACTION = DataAccessorType.register(new ResourceLocation(WiresApi.MOD_ID, "wire_interaction"), DataAccessorType.Builder.createEmptyResponse(
        (in, nbt) -> {
            nbt.put(DataAccessorType.DEFAULT_NBT_DATA, in.toNbt());
        }, (nbt) -> {
            return WireInteractionData.fromNbt(nbt.getCompound(DataAccessorType.DEFAULT_NBT_DATA)).orElse(null);
        }, (player, in, temp, nbt, iteration) -> {
            if (in == null)
                return false;            

            DragonLib.getCurrentServer().ifPresent(x -> {
                x.execute(() -> {
                    InteractionResult interactionresult = null;
                    if (player.getItemInHand(in.hand()).getItem() instanceof IWireInteractableItem i) {
                        interactionresult = i.interactWithWire(player.level(), player, in.hand(), in.hit());
                    }
                    if (interactionresult == null || !interactionresult.consumesAction()) {
                        interactionresult = in.hit().getWireId().type().use(player.level(), player, in.hand(), in.hit());
                    }
                });
            });
            
            return false;
        }
    ));
}
