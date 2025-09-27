package de.mrjulsen.wires.item;

import net.minecraft.nbt.CompoundTag;

public record CustomData(CompoundTag nbt) {
    
    private static final String NBT_POINTS = "CustomPointData";
    private static final String NBT_CUSTOM_DATA = "CustomData";

    public CompoundTag getCustomDataForPoint(int index) {
        if (nbt().contains(NBT_POINTS)) {
            CompoundTag pointsNbt = nbt().getCompound(NBT_POINTS);
            return pointsNbt.contains(String.valueOf(index)) ? pointsNbt.getCompound(String.valueOf(index)) : new CompoundTag();
        }
        return new CompoundTag();
    }
    
    public void setCustomDataForPoint(int index, CompoundTag nbt) {
        CompoundTag pointsNbt = new CompoundTag();
        if (nbt().contains(NBT_POINTS)) {
            pointsNbt = nbt().getCompound(NBT_POINTS);
        }            
        pointsNbt.put(String.valueOf(index), nbt);
        nbt().put(NBT_POINTS, pointsNbt);
    }

    public CompoundTag getCommonData() {
        if (nbt().contains(NBT_CUSTOM_DATA)) {
            return nbt().getCompound(NBT_CUSTOM_DATA);
        }
        return new CompoundTag();
    }

    public void setCommonData(CompoundTag nbt) {
        nbt().put(NBT_CUSTOM_DATA, nbt);
    }

    public boolean hasPoint(int index) {
        if (nbt().contains(NBT_POINTS)) {
            CompoundTag pointsNbt = nbt().getCompound(NBT_POINTS);
            return pointsNbt.contains(String.valueOf(index));
        }
        return false;
    }
}
