package de.mrjulsen.wires.graph.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class DLStaticRegistry<T extends IStaticRegisterable<T>> {
    private final LinkedHashMap<ResourceLocation, DLStaticRegistryObject<? extends T>> TYPES = new LinkedHashMap<>();

    public <S extends T> DLStaticRegistryObject<S> register(ResourceLocation id, Supplier<S> factory) {
        DLStaticRegistryObject<S> type = new DLStaticRegistryObject<>(id, Suppliers.memoize(factory));
        TYPES.put(id, type);
        return type;
    }

    private DLStaticRegistryObject<T> get(ResourceLocation id) {
        return (DLStaticRegistryObject<T>)TYPES.get(id);
    }

    public T load(CompoundTag tag) {
        return DLStaticRegistryObject.load(tag, this::get);
    }

    public Optional<T> getById(ResourceLocation id) {
        if (!TYPES.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(id).get());
    }

    public List<T> getAll() {
        List<T> result = new ArrayList<>(TYPES.size());
        for (DLStaticRegistryObject<? extends T> v : TYPES.values()) {
            result.add(v.get());
        }
        return result;
    }

}
