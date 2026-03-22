package de.mrjulsen.wires.item;

import de.mrjulsen.paw.components.WireConnectionDataComponent;
import de.mrjulsen.paw.registry.ModDataComponents;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireGraph.CreateEdgeResult;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.core.component.DataComponentType;
import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.data.WireHitResult;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Basic item class for a specific {@code IWireType} with basic functionality.
 */
public interface IWireItemBase extends IWireInteractableItem {
    
    public static final String NBT_ROOT = "ConnectionData";
    public static final String NBT_POINTS = "CustomPointData";
    //public static final String NBT_CONNECTOR_TYPE = "ConnectorType";
    public static final String NBT_POS = "Pos";
    //public static final String NBT_WIRE_ID = "WireId";
    public static final String NBT_CUSTOM_DATA = "CustomData";
    public static final String NBT_TOTAL_POINTS_COUNT = "PointsCount";

    /**
     * The type of this wire item.
     * @return The {@code IWireType}
     */
    IWireType getWireType(ItemStack stack);

    default IWireItemBase getActor(ItemStack stack) {
        return this;
    }
    

    default InteractionResultHolder<ItemStack> useWire(Level level, Player player, InteractionHand usedHand) {
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    default InteractionResult useWireOn(UseOnContext context) {        
        return placeWire(
            context.getLevel(),
            context.getPlayer(),
            context.getHand(),
            new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside()),
            null
        );
    }

    @Override
    default InteractionResult interactWithWire(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        return placeWire(level, player, hand, hit, null);
    }

    default boolean addNewPoint(Level level, Player player, InteractionHand hand, HitResult hit, BiConsumer<CompoundTag, CompoundTag> metadata, ItemStack stack, WireConnectionDataComponent connectionData, List<CompoundTag> points) {
        NodeData data = createNodeData(level, player, hand, hit);
        if (data == null) {
            return false;
        }

        // Additional checks
        DLStatus testResult = testPoint(level, player, hand, hit, metadata, stack, connectionData, points, data);
        if (testResult.flag() != DLStatus.FLAG_OK) {
            player.displayClientMessage(TextUtils.translate(testResult.message(), getWireType(stack).getMaxLength()).withStyle(ChatFormatting.RED), true);
            clear(stack);
            return false;
        }

        CompoundTag nodeMeta = new CompoundTag();
        DLUtils.doIfNotNull(metadata, x -> x.accept(connectionData.customData(), nodeMeta));

        CompoundTag nodeData = data.getRegistryType().wrap(data);
        nodeData.put(NBT_CUSTOM_DATA, nodeMeta);
        points.add(nodeData);
        return true;
    }

    default DLStatus testPoint(Level level, Player player, InteractionHand hand, HitResult hit, BiConsumer<CompoundTag, CompoundTag> metadata, ItemStack stack, WireConnectionDataComponent connectionData, List<CompoundTag> points, NodeData nodeData) {
        WireGraph graph = WireGraphManager.get(level, getWireType(stack).getGraphId(connectionData));
        
        if (!points.isEmpty()) {
            NodeData previousNode = WiresApi.NODE_DATA_REGISTRY.load(points.get(points.size() - 1));
            if (previousNode.toWorldPos(graph).distance(nodeData.toWorldPos(graph)) > getWireType(stack).getMaxLength()) {
                return new DLStatus(DLStatus.FLAG_ERROR, 0, "item." + WiresApi.MOD_ID + ".wire.to_far_away");
            } else if (previousNode.equals(nodeData)) {
                return new DLStatus(DLStatus.FLAG_ERROR, 0, "item." + WiresApi.MOD_ID + ".wire.same_connector");
            }
        }
        if (!nodeData.validate(graph, connectionData, points.size())) {
            return new DLStatus(DLStatus.FLAG_ERROR, 0, "item." + WiresApi.MOD_ID + ".wire.connector_invalid");
        }
        return new DLStatus(DLStatus.FLAG_OK, 0, "");
    }

    default boolean canCreateWire(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, WireConnectionDataComponent connectionData, List<CompoundTag> points) {
        return points.size() >= 2;
    }

    
    default CreateEdgeResult createWire(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, WireConnectionDataComponent connectionData, List<CompoundTag> points) {
        WireGraph graph = WireGraphManager.get(level, getWireType(stack).getGraphId(connectionData));
        List<NodeData> deserializedData = new ArrayList<>(points.size());
        CompoundTag pointsMeta = new CompoundTag();

        for (int i = 0; i < points.size(); i++) {
            CompoundTag nodeNbt = points.get(i);
            pointsMeta.put(String.valueOf(i), nodeNbt.getCompound(NBT_CUSTOM_DATA));
            deserializedData.add(WiresApi.NODE_DATA_REGISTRY.load(nodeNbt));
        }

        CompoundTag metaCollection = new CompoundTag();
        if (!connectionData.customData().isEmpty()) metaCollection.put(NBT_CUSTOM_DATA, connectionData.customData());
        if (!pointsMeta.isEmpty()) metaCollection.put(NBT_POINTS, pointsMeta);
        metaCollection.putInt(NBT_TOTAL_POINTS_COUNT, points.size());

        MutableInt idx = new MutableInt();
        CreateEdgeResult result = graph.createEdge(getWireType(stack), new CustomData(metaCollection), deserializedData.get(0), deserializedData.get(1), idx, true);
        if (!result.success()) {
            String key = switch (result.code()) {
                case CreateEdgeResult.CONNECTION_EXISTS -> "item." + WiresApi.MOD_ID + ".wire.connection_already_exists";
                case CreateEdgeResult.INVALID_CONNECTOR -> "item." + WiresApi.MOD_ID + ".wire.connector_invalid";
                default -> "";
            };
            player.displayClientMessage(TextUtils.translate(key).withStyle(ChatFormatting.RED), true);
        }
        clear(stack);
        return result;
    }

    default void removeWireItem(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, int length) {
        if (player == null || (!player.isCreative() && !player.isSpectator())) {
            stack.shrink(1);
        }
    }

    default InteractionResult placeWire(Level level, Player player, InteractionHand hand, HitResult hit, BiConsumer<CompoundTag, CompoundTag> metadata) { 
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof IWireItemBase)) {
            return InteractionResult.FAIL;
        }

        // --- Decode Item data ---
        WireConnectionDataComponent connectionData = ModDataComponents.getComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA, WireConnectionDataComponent::empty);
        List<CompoundTag> points = new ArrayList<>(connectionData.customPointData());

        // --- Set data ---
        if (!addNewPoint(level, player, hand, hit, metadata, stack, connectionData, points)) {
            return InteractionResult.FAIL;
        }

        // --- Save data ---
        connectionData = new WireConnectionDataComponent(points, connectionData.customData());
        ModDataComponents.setComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA, connectionData);

        // --- Create wire ---
        if (canCreateWire(level, player, hand, hit, stack, connectionData, points)) {
            CreateEdgeResult result = createWire(level, player, hand, hit, stack, connectionData, points);
            if (result.success()) {
                removeWireItem(level, player, hand, hit, stack, result.edge().get().length());
            }
        }
        
        return InteractionResult.SUCCESS;
    }

    default NodeData createNodeData(Level level, Player player, InteractionHand hand, HitResult hit) {
        return null;
    }

    /**
     * The text rendered above the hotbar when creating a wire connection which displays information, such as the first connection point and the current distance to that point.
     * @param stack The itemstack
     * @param hit The current location the player is looking at
     * @return The text component which is displayed in the HUD
     */
    default Component createHudInfoText(ItemStack stack, Player player, HitResult hit) {
        if (!ModDataComponents.hasComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA)) {
            return null;
        }

        WireConnectionDataComponent connectionData = ModDataComponents.getComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA, WireConnectionDataComponent::empty);
        List<CompoundTag> points = new ArrayList<>(connectionData.customPointData());
        WireGraphClient graph = WireGraphManager.getClient(player.level(), getWireType(stack).getGraphId(connectionData));
        if (graph == null || points.isEmpty()) {
            return null;
        }

        CompoundTag lastPointData = points.getLast();
        NodeData node = WiresApi.NODE_DATA_REGISTRY.load(lastPointData);
        Vector3d pos = node.toWorldPos(graph);

        int maxLength = getWireType(stack).getMaxLength();
        int distance;
        if (hit instanceof BlockHitResult r) {
            distance = (int)pos.distance(new Vector3d(r.getLocation().x(), r.getLocation().y(), r.getLocation().z()));
        } else {
            distance = (int)pos.distance(new Vector3d(player.getEyePosition().x(), player.getEyePosition().y(), player.getEyePosition().z()));
        }
        return TextUtils.empty().withStyle(ChatFormatting.WHITE)
            .append(TextUtils.text(String.format("X: %s, Y: %s, Z: %s", (int)pos.x(), (int)pos.y(), (int)pos.z())).withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(" \u25A0 ").withStyle(ChatFormatting.GRAY))
            .append(TextUtils.text(String.format("%sm / %sm", distance, getWireType(stack).getMaxLength())).withStyle(distance == maxLength ? ChatFormatting.GOLD : (distance < maxLength ? ChatFormatting.GREEN : ChatFormatting.RED)))
        ;
    }

    default void clear(ItemStack stack) {
        ModDataComponents.setComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA, WireConnectionDataComponent.empty());
    }
}
