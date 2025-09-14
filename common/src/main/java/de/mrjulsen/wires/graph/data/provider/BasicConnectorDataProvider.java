package de.mrjulsen.wires.graph.data.provider;

import org.joml.Vector3f;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;

public class BasicConnectorDataProvider extends ConnectorDataProvider {

    public static final String NBT_WIRE_ATTACH_POINT = "WireAttachPoint";

    protected Vector3f attachOffset;

    public BasicConnectorDataProvider(Vector3f attachOffset) {
        this.attachOffset = attachOffset;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        Utils.putNbtVector3f(nbt, NBT_WIRE_ATTACH_POINT, attachOffset);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        this.attachOffset = Utils.getNbtVector3f(nbt, NBT_WIRE_ATTACH_POINT);
    }

    @Override
    public DLRegistryObject<ConnectorDataProvider> getRegistryType() {
        return (DLRegistryObject<ConnectorDataProvider>)(Object)WiresApi.BASIC_WIRE_CONNECTOR;
    }


    public Vector3f getAttachOffset() {
        return new Vector3f(attachOffset);
    }
    
}
