package de.mrjulsen.wires.graph;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.util.GraphId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The base interface for all wire graphs, which provides some basic structures. By default it is used by {@link WireGraph} or {@link WireGraphClient}.
 */
public interface IWireGraph {
    /**
     * @return The level this graph is valid for.
     */
    Level getLevel();

    /**
     * @return The id of the graph.
     */
    GraphId getId();

    /**
     * Debug only!
     */
    default DLStatistics getStatistics() {
        return DLStatistics.EMPTY;
    }

    /**
     * @param id The id of the node.
     * @return The node with the specified ID, or {@code null} if no node with that ID exists
     */
    WireNode getNode(UUID id);

    /**
     * @param id The id of the edge.
     * @return The edge with the specified ID, or {@code null} if no edge with that ID exists
     */
    WireEdge getEdge(UUID id);

    /**
     * @return A list with all nodes.
     */
    Collection<WireNode> getNodes();

    /**
     * @return A list with all edges.
     */
    Collection<WireEdge> getEdges();

    /**
     * Checks if there is a node with the specified ID.
     * @param id The id of the node.
     * @return {@code true} if a node exists, {@code false} otherwise.
     */
    boolean hasNode(UUID id);
    
    /**
     * Checks if there is a edge with the specified ID.
     * @param id The id of the edge.
     * @return {@code true} if a edge exists, {@code false} otherwise.
     */
    boolean hasEdge(UUID id);

    <A extends NodeAccessor<?>> Optional<A> accessNodesOfType(ResourceLocation typeId);
}
