package de.mrjulsen.wires.graph.data.provider;

import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;

public class BasicConnectorDataProvider extends ConnectorDataProvider {

    public static final String NBT_WIRE_ATTACH_POINT = "WireAttachPoint";

    protected Vector3d attachOffset;


    @Deprecated
    public BasicConnectorDataProvider(Vector3f attachOffset) {
        this(new Vector3d(attachOffset.x(), attachOffset.y(), attachOffset.z()));
    }

    public BasicConnectorDataProvider(Vector3d attachOffset) {
        this.attachOffset = attachOffset;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = new CompoundTag();
        Utils.putNbtVector3d(nbt, NBT_WIRE_ATTACH_POINT, attachOffset);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        this.attachOffset = Utils.getNbtVector3d(nbt, NBT_WIRE_ATTACH_POINT); // TODO
    }

    @Override
    public DLRegistryObject<ConnectorDataProvider> getRegistryType() {
        return (DLRegistryObject<ConnectorDataProvider>)(Object)WiresApi.BASIC_WIRE_CONNECTOR;
    }


    public Vector3d getAttachOffset() {
        return new Vector3d(attachOffset);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicConnectorDataProvider o) {
            return attachOffset.equals(o.attachOffset);
        }
        return false;
    }
    
}
