package de.mrjulsen.wires.graph.registry;

import java.util.function.Function;
import java.util.function.Supplier;

import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final record DLRegistryObject<T extends INBTSerializable>(ResourceLocation id, Supplier<T> factory) {

    public static final String NBT_ID = "Id";
    public static final String NBT_DATA = "Content";

    public CompoundTag wrap(T data) {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ID, id.toString());
        CompoundTag metaData = data.serializeNbt();
        if (metaData != null && !metaData.isEmpty()) tag.put(NBT_DATA, metaData);
        return tag;
    }

    public T unwrap(CompoundTag tag) {
        T instance = create();
        instance.deserializeNbt(tag.contains(NBT_DATA) ? tag.getCompound(NBT_DATA) : new CompoundTag());
        return instance;
    }
    
    public static <T extends INBTSerializable> T load(CompoundTag tag, Function<ResourceLocation, DLRegistryObject<T>> registryGetter) {
        ResourceLocation typeId = DLUtils.resourceLocation(tag.getString(NBT_ID));
        DLRegistryObject<T> type = registryGetter.apply(typeId);
        if (type == null) return null;
        return type.unwrap(tag);
    }

    public T create() {
        return factory.get();
    }
}

