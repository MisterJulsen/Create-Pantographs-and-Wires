package de.mrjulsen.wires.graph.registry;

import java.util.function.Function;
import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final record DLRegistryObject<T extends INBTSerializable>(ResourceLocation id, Supplier<T> factory) {

    public static final String NBT_ID = "Id";
    public static final String NBT_DATA = "Content";

    public CompoundTag wrap(T data) {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ID, id.toString());
        tag.put(NBT_DATA, data.serializeNbt());
        return tag;
    }

    public T unwrap(CompoundTag tag) {
        T instance = create();
        instance.deserializeNbt(tag.getCompound(NBT_DATA));
        return instance;
    }
    
    public static <T extends INBTSerializable> T load(CompoundTag tag, Function<ResourceLocation, DLRegistryObject<T>> registryGetter) {
        ResourceLocation typeId = new ResourceLocation(tag.getString(NBT_ID));
        DLRegistryObject<T> type = registryGetter.apply(typeId);
        if (type == null) return null;
        return type.unwrap(tag);
    }

    public T create() {
        return factory.get();
    }
}

