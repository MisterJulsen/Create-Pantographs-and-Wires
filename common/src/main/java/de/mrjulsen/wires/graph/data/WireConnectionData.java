package de.mrjulsen.wires.graph.data;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.WireBaseItem.CustomData;
import net.minecraft.nbt.CompoundTag;

/**
 * A collection of various custom data. This includes custom data from the item when placing the wire, as well as data from the involved connection points.
 */
public record WireConnectionData(CustomData customData, ConnectorDataProvider connectorA, ConnectorDataProvider connectorB) {

    private static final String NBT_ITEM_DATA = "CustomData";
    private static final String NBT_CONNECTOR_A = "ConnectorA";
    private static final String NBT_CONNECTOR_B = "ConnectorB";

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_ITEM_DATA, customData.nbt());
        nbt.put(NBT_CONNECTOR_A, connectorA.getRegistryType().wrap(connectorA));
        nbt.put(NBT_CONNECTOR_B, connectorB.getRegistryType().wrap(connectorB));
        return nbt;
    }

    public static WireConnectionData fromNbt(CompoundTag nbt) {
        return new WireConnectionData(
            new CustomData(nbt.getCompound(NBT_ITEM_DATA)),
            WiresApi.CONNECTOR_DATA_PROVIDER_REGISTRY.load(nbt.getCompound(NBT_CONNECTOR_A)), 
            WiresApi.CONNECTOR_DATA_PROVIDER_REGISTRY.load(nbt.getCompound(NBT_CONNECTOR_B))
        );
    }
}
