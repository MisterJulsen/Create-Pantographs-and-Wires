package de.mrjulsen.paw.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public final class Utils {
    public static void putNbtVec3(CompoundTag compound, String name, Vec3 vec) {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("X", vec.x());
        nbt.putDouble("Y", vec.y());
        nbt.putDouble("Z", vec.z());
        compound.put(name, nbt);
    }

    public static Vec3 getNbtVec3(CompoundTag compound, String name) {
        CompoundTag nbt = compound.getCompound(name);
        return new Vec3(nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
