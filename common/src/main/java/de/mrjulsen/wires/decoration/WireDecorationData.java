package de.mrjulsen.wires.decoration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class WireDecorationData implements Comparable<WireDecorationData> {

    private static final String NBT_WIRE_NAME = "WireName";
    private static final String NBT_POS = "Pos";
    private static final String NBT_DATA = "Data";
    private static final String NBT_ID = "Id";
    private static final String NBT_TAG = "Data";

    private final String wireName;
    private final float wirePos; // 1D Coordinate on the wire
    private final WireDecorationElement<?> decoration;

    public WireDecorationData(String wireName, float wirePos, WireDecorationElement<?> decoration) {
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

    public WireDecorationElement<?> getDecoration() {
        return decoration;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_WIRE_NAME, wireName);
        nbt.putFloat(NBT_POS, wirePos);
        CompoundTag decorationNbt = new CompoundTag();
        decorationNbt.putString(NBT_ID, decoration.getId().toString());
        CompoundTag meta = new CompoundTag();
        decoration.writeNbt(meta);
        decorationNbt.put(NBT_TAG, meta);
        nbt.put(NBT_DATA, decorationNbt);
        return nbt;
    }

    public static WireDecorationData fromNbt(CompoundTag nbt) {
        CompoundTag decorationNbt = nbt.getCompound(NBT_DATA);
        WireDecorationElement<?> decoration = WireDecorationRegistry.get(new ResourceLocation(decorationNbt.getString(NBT_ID))).get();
        decoration.readNbt(decorationNbt.getCompound(NBT_TAG));
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
