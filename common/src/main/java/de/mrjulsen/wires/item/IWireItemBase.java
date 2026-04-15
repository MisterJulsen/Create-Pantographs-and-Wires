package de.mrjulsen.wires.item;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.client.VerticalPlaneOutline;
import de.mrjulsen.wires.graph.*;
import de.mrjulsen.wires.graph.WireGraph.CreateEdgeResult;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

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
    public static final String NBT_CONNECTOR_TYPE = "ConnectorType";
    public static final String NBT_POS = "Pos";
    public static final String NBT_WIRE_ID = "WireId";
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
        return InteractionResultHolder.pass(player.getItemInHand(usedHand));
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

    default boolean addNewPoint(Level level, Player player, InteractionHand hand, HitResult hit, BiConsumer<CompoundTag, CompoundTag> metadata, ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points) {
        NodeData data = createNodeData(level, player, hand, hit);
        if (data == null) {
            return false;
        }

        // Additional checks
        DLStatus testResult = testPoint(level, player, hand, hit, metadata, stack, itemData, customDataNbt, points, data);
        if (testResult.flag() != DLStatus.FLAG_OK) {
            player.displayClientMessage(TextUtils.translate(testResult.message(), getWireType(stack).getMaxLength()).withStyle(ChatFormatting.RED), true);
            clear(null, stack);
            return false;
        }

        CompoundTag nodeMeta = new CompoundTag();
        DLUtils.doIfNotNull(metadata, x -> x.accept(customDataNbt, nodeMeta));

        CompoundTag nodeData = data.getRegistryType().wrap(data);
        nodeData.put(NBT_CUSTOM_DATA, nodeMeta);
        points.add(nodeData);
        return true;
    }

    default DLStatus testPoint(Level level, Player player, InteractionHand hand, HitResult hit, BiConsumer<CompoundTag, CompoundTag> metadata, ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points, NodeData nodeData) {
        WireGraph graph = WireGraphManager.get(level, getWireType(stack).getGraphId(itemData));
        
        if (!points.isEmpty()) {
            NodeData previousNode = WiresApi.NODE_DATA_REGISTRY.load(points.get(points.size() - 1));
            if (previousNode.toWorldPos(graph).distance(nodeData.toWorldPos(graph)) > getWireType(stack).getMaxLength()) {
                return new DLStatus(DLStatus.FLAG_ERROR, 0, "item." + WiresApi.MOD_ID + ".wire.to_far_away");
            } else if (previousNode.equals(nodeData)) {
                return new DLStatus(DLStatus.FLAG_ERROR, 0, "item." + WiresApi.MOD_ID + ".wire.same_connector");
            }
        }
        if (!nodeData.validate(graph, itemData, points.size())) {
            return new DLStatus(DLStatus.FLAG_ERROR, 0, "item." + WiresApi.MOD_ID + ".wire.connector_invalid");
        }
        return new DLStatus(DLStatus.FLAG_OK, 0, "");
    }

    default boolean canCreateWire(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points) {
        return points.size() >= 2;
    }

    
    default CreateEdgeResult createWire(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points) {
        WireGraph graph = WireGraphManager.get(level, getWireType(stack).getGraphId(itemData));
        List<NodeData> deserializedData = new ArrayList<>(points.size());
        CompoundTag pointsMeta = new CompoundTag();

        for (int i = 0; i < points.size(); i++) {
            CompoundTag nodeNbt = points.get(i);
            pointsMeta.put(String.valueOf(i), nodeNbt.getCompound(NBT_CUSTOM_DATA));
            deserializedData.add(WiresApi.NODE_DATA_REGISTRY.load(nodeNbt));
        }

        CompoundTag metaCollection = new CompoundTag();
        if (!customDataNbt.isEmpty()) metaCollection.put(NBT_CUSTOM_DATA, customDataNbt);
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
        clear(null, stack);
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
        CompoundTag itemData = getNbt(stack);
        CompoundTag customDataNbt = itemData.getCompound(NBT_CUSTOM_DATA);
        List<CompoundTag> points = new ArrayList<>();        
        if (itemData.contains(NBT_POINTS)) {
            points.addAll(itemData.getList(NBT_POINTS, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList());
        }

        // --- Set data ---
        if (!addNewPoint(level, player, hand, hit, metadata, stack, itemData, customDataNbt, points)) {
            return InteractionResult.FAIL;
        }

        // --- Save data ---
        ListTag pointsList = new ListTag();
        for (CompoundTag p : points) {
            pointsList.add(p);
        }
        itemData.put(NBT_POINTS, pointsList);
        itemData.put(NBT_CUSTOM_DATA, customDataNbt);
        setNbt(stack, itemData);

        // --- Create wire ---
        if (canCreateWire(level, player, hand, hit, stack, itemData, customDataNbt, points)) {
            CreateEdgeResult result = createWire(level, player, hand, hit, stack, itemData, customDataNbt, points);
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
        if (!stack.hasTag() || !stack.getTag().contains(NBT_ROOT)) {
            return null;
        }
        
        CompoundTag itemData = getNbt(stack);
        ListTag list = itemData.getList(NBT_POINTS, Tag.TAG_COMPOUND);
        WireGraphClient graph = WireGraphManager.getClient(player.level(), getWireType(stack).getGraphId(itemData));
        if (graph == null || list.isEmpty()) {
            return null;
        }

        CompoundTag lastPointData = (CompoundTag)list.get(list.size() - 1);
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

    default void renderHelperOutline(ItemStack stack, Player player, HitResult hit) {

        if (!stack.hasTag() || !stack.getTag().contains(NBT_ROOT)) {
            return;
        }

        CompoundTag itemData = getNbt(stack);
        ListTag list = itemData.getList(NBT_POINTS, Tag.TAG_COMPOUND);
        WireGraphClient graph = WireGraphManager.getClient(player.level(), getWireType(stack).getGraphId(itemData));
        if (graph == null || list.isEmpty()) {
            return;
        }

        CompoundTag lastPointData = (CompoundTag)list.get(0);
        NodeData node = WiresApi.NODE_DATA_REGISTRY.load(lastPointData);



        CompoundTag customDataNbt = itemData.getCompound(NBT_CUSTOM_DATA);

        CompoundTag pointsMeta = new CompoundTag();
        List<CompoundTag> points = new ArrayList<>();
        if (itemData.contains(NBT_POINTS)) {
            points.addAll(itemData.getList(NBT_POINTS, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList());
        }
        for (int i = 0; i < points.size(); i++) {
            CompoundTag nodeNbt = points.get(i);
            pointsMeta.put(String.valueOf(i), nodeNbt.getCompound(NBT_CUSTOM_DATA));
        }


        NodeData selectionNode = createNodeData(player.level(), player, InteractionHand.MAIN_HAND, hit);
        if (selectionNode != null) {
            CompoundTag nodeData = selectionNode.getRegistryType().wrap(selectionNode);
            nodeData.put(NBT_CUSTOM_DATA, new CompoundTag());
            points.add(nodeData);

            CompoundTag metaCollection = new CompoundTag();
            if (!customDataNbt.isEmpty()) metaCollection.put(NBT_CUSTOM_DATA, customDataNbt);
            if (!pointsMeta.isEmpty()) metaCollection.put(NBT_POINTS, pointsMeta);
            metaCollection.putInt(NBT_TOTAL_POINTS_COUNT, points.size());
            CustomData data = new CustomData(metaCollection);
            Optional<ConnectorDataProvider> connectorDataA = node.getConnectorCustomData(graph, data, 0);

            Vector3d posA = node.toWorldPos(graph).add(connectorDataA.map(x -> x.getAsTypeIfMatching(BasicConnectorDataProvider.class).map(BasicConnectorDataProvider::getAttachOffset).orElse(new Vector3d())).orElse(new Vector3d()));

            Optional<ConnectorDataProvider> connectorDataB = selectionNode.getConnectorCustomData(graph, data, 1);
            Vector3d posB = selectionNode.toWorldPos(graph).add(connectorDataB.map(x -> x.getAsTypeIfMatching(BasicConnectorDataProvider.class).map(BasicConnectorDataProvider::getAttachOffset).orElse(new Vector3d())).orElse(new Vector3d()));

            Outliner.getInstance().showOutline(this, new VerticalPlaneOutline(posA, posB))
                    .colored(Color.SPRING_GREEN);
        }


    }


    public static CompoundTag getNbt(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        CompoundTag root;
        if (!nbt.contains(NBT_ROOT)) {
            root = new CompoundTag();
        } else {
            root = nbt.getCompound(NBT_ROOT);
        }
        if (!root.contains(NBT_POINTS)) {
            root.put(NBT_POINTS, new ListTag());
        }
        nbt.put(NBT_ROOT, root);
        return root;
    }

    public static void setNbt(ItemStack stack, CompoundTag nbt) {
        CompoundTag itemNbt = stack.getOrCreateTag();
        itemNbt.put(NBT_ROOT, nbt);
        stack.setTag(itemNbt);
    }


    public static void clear(@Nullable Player player, ItemStack stack) {
        stack.getOrCreateTag().remove(NBT_ROOT);
        if (player != null)
            player.displayClientMessage(TextUtils.translate("item." + PantographsAndWires.MOD_ID + ".wire.clear_settings").withStyle(ChatFormatting.RED), true);
    }
}
