package de.mrjulsen.wires.util;

import org.joml.Vector3d;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

public final class Utils {

    public static void putNbtBlockPos(CompoundTag compound, String name, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("X", pos.getX());
        nbt.putInt("Y", pos.getY());
        nbt.putInt("Z", pos.getZ());
        compound.put(name, nbt);
    }

    public static SectionPos getNbtSectionPos(CompoundTag compound, String name) {
        CompoundTag nbt = compound.getCompound(name);
        return SectionPos.of(nbt.getInt("X"), nbt.getInt("Y"), nbt.getInt("Z"));
    }
    
    public static void putNbtSectionPos(CompoundTag compound, String name, SectionPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("X", pos.getX());
        nbt.putInt("Y", pos.getY());
        nbt.putInt("Z", pos.getZ());
        compound.put(name, nbt);
    }

    public static ChunkPos getNbtChunkPos(CompoundTag compound, String name) {
        CompoundTag nbt = compound.getCompound(name);
        return new ChunkPos(nbt.getInt("X"), nbt.getInt("Z"));
    }
    
    public static void putNbtChunkPos(CompoundTag compound, String name, ChunkPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("X", pos.x);
        nbt.putInt("Z", pos.z);
        compound.put(name, nbt);
    }

    public static BlockPos getNbtBlockPos(CompoundTag compound, String name) {
        CompoundTag nbt = compound.getCompound(name);
        return new BlockPos(nbt.getInt("X"), nbt.getInt("Y"), nbt.getInt("Z"));
    }
    
    public static void putNbtVector3f(CompoundTag compound, String name, Vector3f vec) {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("X", vec.x());
        nbt.putDouble("Y", vec.y());
        nbt.putDouble("Z", vec.z());
        compound.put(name, nbt);
    }
    
    public static void putNbtVector3d(CompoundTag compound, String name, Vector3d vec) {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("X", vec.x());
        nbt.putDouble("Y", vec.y());
        nbt.putDouble("Z", vec.z());
        compound.put(name, nbt);
    }

    public static Vector3f getNbtVector3f(CompoundTag compound, String name) {
        CompoundTag nbt = compound.getCompound(name);
        return new Vector3f((float)nbt.getDouble("X"), (float)nbt.getDouble("Y"), (float)nbt.getDouble("Z"));
    }

    public static Vector3d getNbtVector3d(CompoundTag compound, String name) {
        CompoundTag nbt = compound.getCompound(name);
        return new Vector3d(nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
    }

    public boolean isSectionInChunk(SectionPos section, ChunkPos chunk) {
        return section.getX() == chunk.x && section.getZ() == chunk.z;
    }

    public ChunkPos getChunkOfSection(SectionPos section) {
        return new ChunkPos(section.getX(), section.getZ());
    }

    
}
