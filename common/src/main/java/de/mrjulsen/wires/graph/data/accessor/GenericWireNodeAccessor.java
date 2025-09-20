package de.mrjulsen.wires.graph.data.accessor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.node.INodeDataWire;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.network.WireId;

public class GenericWireNodeAccessor<T extends NodeData & INodeDataWire> extends NodeAccessor<T> {

    private final Multimap<WireId, WireNode> nodesByWire = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Multimap<UUID, WireNode> nodesByEdge = MultimapBuilder.hashKeys().arrayListValues().build();

    public GenericWireNodeAccessor() {}

    @Override
    public void put(WireNode node) {
        this.nodesByWire.put(getNodeData(node).getWireId(), node);
        this.nodesByEdge.put(getNodeData(node).getWireId().id(), node);
    }

    @Override
    public void remove(WireNode node) {
        nodesByWire.values().removeIf(x -> x.equals(node));
        nodesByEdge.values().removeIf(x -> x.equals(node));
    }

    public Collection<WireNode> get(WireId id) {
        if (nodesByWire.containsKey(id)) {
            return Collections.unmodifiableCollection(nodesByWire.get(id));
        }
        return List.of();
    }

    public Collection<WireNode> get(UUID edgeId) {
        if (nodesByEdge.containsKey(edgeId)) {
            return Collections.unmodifiableCollection(nodesByEdge.get(edgeId));
        }
        return List.of();
    }
}
