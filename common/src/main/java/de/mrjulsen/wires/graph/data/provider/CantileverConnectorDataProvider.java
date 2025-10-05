package de.mrjulsen.wires.graph.data.provider;

import org.joml.Vector3f;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.registry.DLRegistryObject;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.nbt.CompoundTag;

public class CantileverConnectorDataProvider extends BasicConnectorDataProvider {

    public static final String NBT_TENSION_ATTACH_POINT = "TensionWireAttachPoint";

    protected Vector3f tensionWireAttachPoint;

    public CantileverConnectorDataProvider(Vector3f contactWireAttachOffset, Vector3f tensionWireAttachPoint) {
        super(contactWireAttachOffset);
        this.tensionWireAttachPoint = tensionWireAttachPoint;
    }

    @Override
    public CompoundTag serializeNbt() {
        CompoundTag nbt = super.serializeNbt();
        Utils.putNbtVector3f(nbt, NBT_TENSION_ATTACH_POINT, tensionWireAttachPoint);
        return nbt;
    }

    @Override
    public void deserializeNbt(CompoundTag nbt) {
        super.deserializeNbt(nbt);
        this.tensionWireAttachPoint = Utils.getNbtVector3f(nbt, NBT_TENSION_ATTACH_POINT);
    }

    @Override
    public DLRegistryObject<ConnectorDataProvider> getRegistryType() {
        return (DLRegistryObject<ConnectorDataProvider>)(Object)WiresApi.CANTILEVER_WIRE_CONNECTOR;
    }

    public Vector3f getTensionWireAttachOffset() {
        return new Vector3f(tensionWireAttachPoint);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CantileverConnectorDataProvider o) {
            return super.equals(o) && tensionWireAttachPoint.equals(o.tensionWireAttachPoint);
        }
        return false;
    }
    
}
