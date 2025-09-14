package de.mrjulsen.wires.graph.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.node.NodeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class NodeDataRegistry {
    private NodeDataRegistry() {}

    public static final NodeDataRegistry INSTANCE = new NodeDataRegistry();

    private final Map<ResourceLocation, NodeDataRegistryObject<? extends NodeData, ? extends NodeAccessor<?>>> TYPES = new HashMap<>();

    public <S extends NodeData, B extends NodeAccessor<S>> NodeDataRegistryObject<S, B> register(ResourceLocation id, Supplier<S> factory, Supplier<B> accessor) {
        NodeDataRegistryObject<S, B> type = new NodeDataRegistryObject<>(id, factory, accessor);
        TYPES.put(id, type);
        return type;
    }

    private NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> get(ResourceLocation id) {
        return (NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>>)TYPES.get(id);
    }

    public NodeData load(CompoundTag tag) {
        return NodeDataRegistryObject.load(tag, this::get);
    }

}
