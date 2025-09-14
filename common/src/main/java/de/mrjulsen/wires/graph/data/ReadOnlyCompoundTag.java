package de.mrjulsen.wires.graph.data;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class ReadOnlyCompoundTag {
    private final CompoundTag delegate;

    public ReadOnlyCompoundTag(CompoundTag delegate) {
        this.delegate = delegate;
    }

    public Set<String> getAllKeys() {
        return delegate.getAllKeys();
    }

    public Optional<Byte> getByte(String key) {
        return delegate.contains(key, 99)
                ? Optional.of(delegate.getByte(key))
                : Optional.empty();
    }

    public Optional<Short> getShort(String key) {
        return delegate.contains(key, 99)
                ? Optional.of(delegate.getShort(key))
                : Optional.empty();
    }

    public Optional<Integer> getInt(String key) {
        return delegate.contains(key, 99)
                ? Optional.of(delegate.getInt(key))
                : Optional.empty();
    }

    public Optional<Long> getLong(String key) {
        return delegate.contains(key, 99)
                ? Optional.of(delegate.getLong(key))
                : Optional.empty();
    }

    public Optional<Float> getFloat(String key) {
        return delegate.contains(key, 99)
                ? Optional.of(delegate.getFloat(key))
                : Optional.empty();
    }

    public Optional<Double> getDouble(String key) {
        return delegate.contains(key, 99)
                ? Optional.of(delegate.getDouble(key))
                : Optional.empty();
    }

    public Optional<String> getString(String key) {
        return delegate.contains(key, 8)
                ? Optional.of(delegate.getString(key))
                : Optional.empty();
    }

    public Optional<byte[]> getByteArray(String key) {
        return delegate.contains(key, 7)
                ? Optional.of(delegate.getByteArray(key))
                : Optional.empty();
    }

    public Optional<int[]> getIntArray(String key) {
        return delegate.contains(key, 11)
                ? Optional.of(delegate.getIntArray(key))
                : Optional.empty();
    }

    public Optional<long[]> getLongArray(String key) {
        return delegate.contains(key, 12)
                ? Optional.of(delegate.getLongArray(key))
                : Optional.empty();
    }

    public Optional<UUID> getUUID(String key) {
        return delegate.hasUUID(key)
                ? Optional.of(delegate.getUUID(key))
                : Optional.empty();
    }

    public Optional<Boolean> getBoolean(String key) {
        return delegate.contains(key, 99)
                ? Optional.of(delegate.getBoolean(key))
                : Optional.empty();
    }

    public Optional<ReadOnlyCompoundTag> getCompound(String key) {
        return delegate.contains(key, 10)
                ? Optional.of(new ReadOnlyCompoundTag(delegate.getCompound(key)))
                : Optional.empty();
    }

    public Optional<ListTag> getList(String key, int tagType) {
        return delegate.getTagType(key) == 9
                ? Optional.of(delegate.getList(key, tagType))
                : Optional.empty();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}

