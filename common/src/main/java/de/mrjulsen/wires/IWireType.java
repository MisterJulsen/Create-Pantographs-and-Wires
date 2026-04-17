package de.mrjulsen.wires;

import java.util.Optional;

import de.mrjulsen.paw.components.WireConnectionDataComponent;
import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.wires.graph.IWireGraph;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.item.IWireItemBase;
import de.mrjulsen.wires.util.GraphId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;

/**
 * The base for all custom wire types.
 */
public interface IWireType {
    /**
     * Creates all components of the wire connections between two connectors. Can be one single wire or multiple wires.
     * @param context The current build context. Declares whether only the collision data, the rendering data or both is
     * required. Use this to reduce calculations and improving performance by only calculating data that is really needed.
     * If you don't provide the data that is needed, then the wire may not work as expected.
     * @param level The level the wire is in.
     * @return A collection of all wires that should be used in this connection.
     */
    WireBatch buildWire(WireCreationContext context, BlockAndTintGetter level, WireConnectionData customData, WireEdge edge, WireNode nodeA, WireNode nodeB);
    
    /**
     * The maximum length of the wire between two connectors.
     * @return The maximum length of the wire in blocks.
     */
    int getMaxLength();

    /**
     * The ID of this wire type.
     */
    ResourceLocation getRegistryId();

    /**
     * The ID of the registered wire graph in which this wire type should be used. Typically, a wire type can only be used in
     * one graph, but the {@code itemData} allows for the use of different graphs based on the item metadata.
     * @param itemData The item metadata.
     * @return The {@link GraphId}.
     */
    GraphId getGraphId(WireConnectionDataComponent itemData);

    /**
     * Called when a player interacts with a wire of this type. Features such as adding decorations, destroying the wire, and more can be implemented here.
     * @param level The current level.
     * @param player The player interacting with this wire.
     * @param hand The used hand.
     * @param hitResult Information about the interaction.
     * @return The result of this interaction.
     */
    default InteractionResult use(Level level, Player player, InteractionHand hand, WireHitResult hitResult) {
        return InteractionResult.PASS;
    }

    /**
     * Called when the wire is destroyed. Either because one of the connectors is destroyed, a block is placed inside the wire, or by other events
     * that remove the wire. Item drops and more can be implemented here.
     * @param level The current level.
     * @param breakPosition The position where this wire was broken.
     * @param player The player who is responsible for the destruction.
     */
    void onBreak(Level level, Vector3d breakPosition, Optional<Player> player, IWireGraph graph, WireEdge edge);

    /**
     * Called when another wire attempts to connect to this wire. This method then generates the appropriate {@link NodeData} for this wire type.
     * By default, {@code null} is returned, which prevents a connection to this wire type. If a connection to this wire should be allowed, a
     * corresponding {@link NodeData} instance must be returned. NOTE: {@code null} does not completely prevent a connection to this wire!
     * Other {@link IWireType}s can implement their own logic to create a connection according to their own rules.
     * @param level The current level.
     * @param player The player who clicked on the wire.
     * @param hand The used hand.
     * @param hit The {@link WireHitResult} with all information about this wire.
     * @param item Das {@code item} ist das Item des wires, dass sich mit diesem wire verbinden möchte.
     * @return A new instance of {@link WireNode} or {@code null} to prevent wire connections from this side.
     */
    default <I extends Item & IWireItemBase> NodeData attachWireTo(Level level, Player player, InteractionHand hand, WireHitResult hit, I item) {
        return null;
    }
}
