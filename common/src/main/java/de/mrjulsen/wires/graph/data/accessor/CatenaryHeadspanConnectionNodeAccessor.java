package de.mrjulsen.wires.graph.data.accessor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.node.CatenaryHeadspanConnectionNodeData;
import net.minecraft.core.BlockPos;

public class CatenaryHeadspanConnectionNodeAccessor extends NodeAccessor<CatenaryHeadspanConnectionNodeData> {

    private final Multimap<BlockPos, WireNode> nodes = MultimapBuilder.hashKeys().arrayListValues().build();

    public CatenaryHeadspanConnectionNodeAccessor() {}

    @Override
    public void put(WireNode node) {
        //this.nodes.put(getNodeData(node), node);
    }

    @Override
    public void remove(WireNode node) {
        nodes.values().removeIf(x -> x.equals(node));
    }

    public Collection<WireNode> get(BlockPos pos) {
        if (nodes.containsKey(pos)) {
            return Collections.unmodifiableCollection(nodes.get(pos));
        }
        return List.of();
    }
}
