package de.mrjulsen.wires.graph.registry;

import java.util.function.Function;

import com.google.common.base.Supplier;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final record DLStaticRegistryObject<T>(ResourceLocation id, Supplier<T> factory) {

    public static final String NBT_ID = "Id";

    public CompoundTag wrap(T data) {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_ID, id.toString());
        return tag;
    }
    
    public static <T> T load(CompoundTag tag, Function<ResourceLocation, DLStaticRegistryObject<T>> registryGetter) {
        ResourceLocation typeId = DLUtils.resourceLocation(tag.getString(NBT_ID));
        DLStaticRegistryObject<T> type = registryGetter.apply(typeId);
        if (type == null) return null;
        return type.get();
    }

    public T get() {
        return factory.get();
    }
}

