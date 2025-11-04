package de.mrjulsen.wires.network;

import de.mrjulsen.mcdragonlib.network.DLNetworkManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.network.NetworkPacketType;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.network.packets.cts.WireInteractionPacketData;
import de.mrjulsen.wires.network.packets.stc.DeleteWireConnectionPacketData;
import de.mrjulsen.wires.network.packets.stc.WireConnectionChunkUnloadingPacketData;
import de.mrjulsen.wires.network.packets.stc.WireConnectorDataPacketData;

public final class ModNetworkManager {
    private ModNetworkManager() {}

    public static final DLNetworkManager NETWORK = new DLNetworkManager(DLUtils.resourceLocation(WiresApi.MOD_ID, "network"), "1");

    public static final NetworkPacketType.Send<NetworkDirection.S2C, DeleteWireConnectionPacketData> DELETE_WIRE_CONNECTION = NETWORK.registerSendOnlyPacket("delete_wire_connection", NetworkDirection.S2C, DeleteWireConnectionPacketData::handle, DeleteWireConnectionPacketData::new);
    public static final NetworkPacketType.Send<NetworkDirection.S2C, WireConnectionChunkUnloadingPacketData> CONNECTION_CHUNK_UNLOADING = NETWORK.registerSendOnlyPacket("connection_chunk_unloading", NetworkDirection.S2C, WireConnectionChunkUnloadingPacketData::handle, WireConnectionChunkUnloadingPacketData::new);
    public static final NetworkPacketType.Send<NetworkDirection.S2C, WireConnectorDataPacketData> WIRE_CONNECTOR_DATA = NETWORK.registerSendOnlyPacket("wire_connector_data", NetworkDirection.S2C, WireConnectorDataPacketData::handle, WireConnectorDataPacketData::new);
    
    public static final NetworkPacketType.Send<NetworkDirection.C2S, WireInteractionPacketData> WIRE_INTERACTION = NETWORK.registerSendOnlyPacket("wire_interaction", NetworkDirection.C2S, WireInteractionPacketData::handle, WireInteractionPacketData::new);
    

    public static void init() {}
}
