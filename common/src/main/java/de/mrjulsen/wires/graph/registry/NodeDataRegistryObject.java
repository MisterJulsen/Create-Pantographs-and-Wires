package de.mrjulsen.wires.graph.registry;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.node.NodeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final record NodeDataRegistryObject<T extends NodeData, A extends NodeAccessor<T>>(ResourceLocation id, Supplier<T> factory, Supplier<A> accessorFactory) {

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
    
    public static <T extends NodeData> T load(CompoundTag tag, Function<ResourceLocation, NodeDataRegistryObject<T, ?>> registryGetter) {
        ResourceLocation typeId = new ResourceLocation(tag.getString(NBT_ID));
        NodeDataRegistryObject<T, ?> type = registryGetter.apply(typeId);
        if (type == null) return null;
        return type.unwrap(tag);
    }

    public T create() {
        return factory().get();
    }

    public A createAccessor() {
        return accessorFactory().get();
    }

    public Optional<A> getAccessor(IWireGraph graph) {
        return (Optional<A>)graph.accessNodesOfType(id);
    }
}

