package de.mrjulsen.paw.network;

import de.mrjulsen.mcdragonlib.network.DLNetworkManager;
import de.mrjulsen.mcdragonlib.network.NetworkDirection;
import de.mrjulsen.mcdragonlib.network.NetworkPacketType;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.network.packets.ClearWireConnectionPacketData;
import de.mrjulsen.paw.network.packets.UpdateCantileverSettingsPacketData;
import de.mrjulsen.paw.network.packets.UpdateWireSettingsPacketData;

public final class ModNetworkManager {
    private ModNetworkManager() {}

    public static final DLNetworkManager NETWORK = new DLNetworkManager(DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "network"), "1");

    public static final NetworkPacketType.Send<NetworkDirection.C2S, ClearWireConnectionPacketData> CLEAR_WIRE_CONNECTION = NETWORK.registerSendOnlyPacket("clear_wire_connection", NetworkDirection.C2S, ClearWireConnectionPacketData::handle, ClearWireConnectionPacketData::new);
    public static final NetworkPacketType.Send<NetworkDirection.C2S, UpdateCantileverSettingsPacketData> UPDATE_CANTILEVER_SETTINGS = NETWORK.registerSendOnlyPacket("update_cantilever_settings", NetworkDirection.C2S, UpdateCantileverSettingsPacketData::handle, UpdateCantileverSettingsPacketData::new);
    public static final NetworkPacketType.Send<NetworkDirection.C2S, UpdateWireSettingsPacketData> UPDATE_WIRE_SETTINGS = NETWORK.registerSendOnlyPacket("update_wire_settings", NetworkDirection.C2S, UpdateWireSettingsPacketData::handle, UpdateWireSettingsPacketData::new);
    
    public static void init() {}
}
