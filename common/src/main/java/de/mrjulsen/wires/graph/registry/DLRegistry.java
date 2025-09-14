package de.mrjulsen.wires.graph.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class DLRegistry<T extends INBTSerializable & IRegisterable<T>> {
    private final Map<ResourceLocation, DLRegistryObject<? extends T>> TYPES = new HashMap<>();

    public <S extends T> DLRegistryObject<S> register(ResourceLocation id, Supplier<S> factory) {
        DLRegistryObject<S> type = new DLRegistryObject<>(id, factory);
        TYPES.put(id, type);
        return type;
    }

    private DLRegistryObject<T> get(ResourceLocation id) {
        return (DLRegistryObject<T>)TYPES.get(id);
    }

    public T load(CompoundTag tag) {
        return DLRegistryObject.load(tag, this::get);
    }

}
