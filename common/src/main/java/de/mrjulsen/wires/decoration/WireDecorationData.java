package de.mrjulsen.wires.decoration;

import de.mrjulsen.paw.registry.ModWireRegistry;
import net.minecraft.nbt.CompoundTag;

public class WireDecorationData implements Comparable<WireDecorationData> {

    private static final String NBT_WIRE_NAME = "WireName";
    private static final String NBT_POS = "Pos";
    private static final String NBT_DATA = "Data";

    private final String wireName;
    private final float wirePos; // 1D Coordinate on the wire
    private final IWireDecoration<?> decoration;

    public WireDecorationData(String wireName, float wirePos, IWireDecoration<?> decoration) {
        this.wireName = wireName;
        this.wirePos = wirePos;
        this.decoration = decoration;
    }

    public String getWireName() {
        return wireName;
    }

    public float getPos() {
        return wirePos;
    }

    public IWireDecoration<?> getDecoration() {
        return decoration;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_WIRE_NAME, wireName);
        nbt.putFloat(NBT_POS, wirePos);

        CompoundTag decorationNbt = decoration.getRegistryType().wrap(decoration);
        nbt.put(NBT_DATA, decorationNbt);
        return nbt;
    }

    public static WireDecorationData fromNbt(CompoundTag nbt) {
        IWireDecoration<?> decoration = ModWireRegistry.DECORATION_REGISTRY.load(nbt.getCompound(NBT_DATA));
        return new WireDecorationData(
            nbt.getString(NBT_WIRE_NAME),
            nbt.getFloat(NBT_POS),
            decoration
        );
    }

    @Override
    public int compareTo(WireDecorationData o) {
        return Float.compare(wirePos, o.wirePos);
    }
}
