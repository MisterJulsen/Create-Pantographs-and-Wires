package de.mrjulsen.wires.graph.data.node;

import java.util.Optional;

<<<<<<< HEAD
=======
import de.mrjulsen.paw.components.WireConnectionDataComponent;
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.data.INBTSerializable;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.graph.registry.NodeDataRegistryObject;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class NodeData implements INBTSerializable {

    private WireNode node;

    public final WireNode getOrCreateNodeInternal(WireGraph graph, IWireType wireType, CustomData customData) {
        node = getOrCreateNode(graph);
        return node;
    }

    public WireNode getNode() {
        return node;
    }

    public abstract Vector3d toWorldPos(IWireGraph graph);

    public abstract NodeDataRegistryObject<NodeData, NodeAccessor<NodeData>> getRegistryType();


    /**
     * Creates a {@link WireNode} based on the information provided by this object. Depending on the situation, a new node or an
     * existing node (e.g. referenced in another object) can be returned. This data is used for the node itself and is not
     * specific to the wires.
     * @param graph The current {@link WireGraph} being worked on.
     * @return The wire node.
     */
    protected abstract WireNode getOrCreateNode(WireGraph graph);

    /**
     * Updates the node's data based on current events. This method is similar to {@link #getOrCreateNode}, with the difference that no new node
     * is created, but an existing one is edited.
     * This method is primarily used to change the node's position if the connection
     * point changes. It can do nothing, for example, if the node is bound to a fixed block. The data about the connection points
     * or wire connections are NOT covered here! They must be updated in the corresponding metadata sections!
     * @param graph The current {@link WireGraph} being worked on.
     * @param node The wire node.
     * @return The {@code node} itself, if some settings of the node has been changed, a new node, if the node has been changed, or {@code null} if nothing has been changed.
     */
    public WireNode updateWireNode(WireGraph graph, WireNode node) {
        return null;
    }

    /**
     * Retrieves additional data for a connection point, such as the wire attachment position.
     * Can return {@link ConnectorDataProvider.Empty} in case of an error or if additional data is missing. Wire builders
     * should recognize this type and prevent a wire connection if the data is insufficient. This data is used for wires
     * connecting to this node and is not generally valid for this node.
     * @param graph The current {@link WireGraph} being worked on.
     * @param customData Additional data provided by the wire item.
     * @param pointIndex The current index of the node in the wire connection. Normally, this value is 0 or 1
     *  (for two connection points). However, special wire creation operations may allow more than two points for a connection
     *  (e.g. a connection consisting of multiple wires connected to different points). In this case, the index can also take
     *  other values. Please note that counting does not necessarily have to start at 0. This depends on the specific
     *  implementation of the {@link IWireType} and can vary.
     * @return The connector data.
     */
    public abstract Optional<ConnectorDataProvider> getConnectorCustomData(IWireGraph graph, CustomData customData, int pointIndex);

    public abstract boolean validate(WireGraph graph, WireConnectionDataComponent currentItemData, int pointIndex);

    public void onRemove(Level level, Vector3d breakPosition, Optional<Player> player) {}
}
