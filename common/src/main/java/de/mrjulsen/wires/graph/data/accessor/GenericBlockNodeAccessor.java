package de.mrjulsen.wires.graph.data.accessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.node.GenericBlockNodeData;
import net.minecraft.core.BlockPos;

public class GenericBlockNodeAccessor extends NodeAccessor<GenericBlockNodeData> {

    private final Multimap<BlockPos, WireNode> nodes = MultimapBuilder.hashKeys().arrayListValues().build();

    public GenericBlockNodeAccessor() {}

    @Override
    public void put(WireNode node) {
        this.nodes.put(getNodeData(node).getPos(), node);
    }

    @Override
    public void remove(WireNode node) {
        nodes.values().removeIf(x -> x.equals(node));
    }

    public Collection<WireNode> get(BlockPos pos) {
        if (nodes.containsKey(pos)) {
            return new ArrayList<>(nodes.get(pos));
        }
        return List.of();
    }
}
