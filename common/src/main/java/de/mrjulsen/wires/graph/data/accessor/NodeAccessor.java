package de.mrjulsen.wires.graph.data.accessor;

import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.node.NodeData;

/**
 * This object stores and manages access to all nodes in the {@link WireGraph}. In cases where a node needs to be accessed from an
 * independent object without the two sides knowing each other, this accessor can be used via the wire graph to retrieve
 * nodes from outside without requiring a direct reference.
 * <p><b>Example use case:</b> A node is assigned to a block. Any vanilla Minecraft block doesn't know that the node is bound
 * to it. Likewise, the node doesn't know when the block changes or is destroyed. A third party (e.g. a block break event)
 * only knows the position of the destroyed block but can't connect the node and block. This is where a NodeAccessor comes
 * in. Developers can use this event to search one or more wire graphs for nodes of a specific type (e.g. nodes bound to blocks)
 * and decide what to do next based on the event data. Using the data in the accessor, third parties can infer a relationship
 * between two independent objects.</p>
 */
public abstract class NodeAccessor<T extends NodeData> {
    public abstract void put(WireNode node);
    public abstract void remove(WireNode node);  
    
    protected final T getNodeData(WireNode node) {
        return (T)node.getData();
    }
}
