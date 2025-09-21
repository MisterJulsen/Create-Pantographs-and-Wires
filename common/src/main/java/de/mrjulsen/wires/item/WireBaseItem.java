package de.mrjulsen.wires.item;

import de.mrjulsen.wires.util.Utils;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.registry.NodeDataRegistry;
import de.mrjulsen.wires.graph.data.node.GenericBlockNodeData;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.data.WireHitResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Basic item class for a specific {@code IWireType} with basic functionality.
 */
public class WireBaseItem extends Item implements IWireInteractableItem {
    
    private record ConnectionPointData(BlockPos pos, IWireConnector connector) {}

    public static enum EWireConnectorType {
        CONNECTOR(0, "connector"),
        BLOCK(1, "block"),
        WIRE(2, "wire");

        private final int id;
        private final String name;

        private EWireConnectorType(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static EWireConnectorType getById(int id) {
            return Arrays.stream(values()).filter(x -> x.getId() == id).findFirst().orElse(BLOCK);
        }
    }    

    public static record CustomData(CompoundTag nbt) {
        public CompoundTag getCustomDataForPoint(int index) {
            if (nbt().contains(NBT_POINTS)) {
                CompoundTag pointsNbt = nbt().getCompound(NBT_POINTS);
                return pointsNbt.contains(String.valueOf(index)) ? pointsNbt.getCompound(String.valueOf(index)) : new CompoundTag();
            }
            return new CompoundTag();
        }
        
        public void setCustomDataForPoint(int index, CompoundTag nbt) {
            CompoundTag pointsNbt = new CompoundTag();
            if (nbt().contains(NBT_POINTS)) {
                pointsNbt = nbt().getCompound(NBT_POINTS);
            }            
            pointsNbt.put(String.valueOf(index), nbt);
            nbt().put(NBT_POINTS, pointsNbt);
        }

        public CompoundTag getCommonData() {
            if (nbt().contains(NBT_CUSTOM_DATA)) {
                return nbt().getCompound(NBT_CUSTOM_DATA);
            }
            return new CompoundTag();
        }

        public void setCommonData(CompoundTag nbt) {
            nbt().put(NBT_CUSTOM_DATA, nbt);
        }

        public boolean hasPoint(int index) {
            if (nbt().contains(NBT_POINTS)) {
                CompoundTag pointsNbt = nbt().getCompound(NBT_POINTS);
                return pointsNbt.contains(String.valueOf(index));
            }
            return false;
        }
    }

    public static final String NBT_POINTS = "CustomPointData";
    public static final String NBT_CONNECTOR_TYPE = "ConnectorType";
    public static final String NBT_POS = "Pos";
    public static final String NBT_WIRE_ID = "WireId";
    public static final String NBT_CUSTOM_DATA = "CustomData";
    public static final String NBT_TOTAL_POINTS_COUNT = "PointsCount";

    private final IWireType wireType;

    /**
     * Creates a new Wire Item.
     * @param properties The item properties
     * @param wireType The {@code IWireType}
     */
    public WireBaseItem(Properties properties, IWireType wireType) {
        super(properties);
        this.wireType = wireType;
    }

    /**
     * The type of this wire item.
     * @return The {@code IWireType}
     */
    public IWireType getWireType() {
        return wireType;
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {        
        return placeWire(
            context.getLevel(),
            context.getPlayer(),
            context.getHand(),
            new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside()),
            EWireConnectorType.BLOCK,
            null
        );
    }

    @Override
    public InteractionResult interactWithWire(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        return placeWire(level, player, hand, hit, EWireConnectorType.WIRE, null);
    }

    public int getRequiredPoints() {
        return 2;
    }

    protected boolean addNewPoint(Level level, Player player, InteractionHand hand, HitResult hit, EWireConnectorType type, BiConsumer<CompoundTag, CompoundTag> metadata, ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points) {
        WireGraph graph = WireGraphManager.get(level, getWireType().getGraphId(itemData));
        NodeData data = createNodeData(level, player, hand, hit, type);
        if (data == null) {
            return false;
        }

        // Additional checks
        checks: {
            String translationKey = "";
            
            if (!points.isEmpty()) {
                NodeData previousNode = WiresApi.NODE_DATA_REGISTRY.load(points.get(points.size() - 1));
                if (previousNode.toWorldPos(graph).distance(data.toWorldPos(graph)) > getWireType().getMaxLength()) {
                    translationKey = "item." + WiresApi.MOD_ID + ".wire.to_far_away";
                } else if (previousNode.equals(data)) {
                    translationKey = "item." + WiresApi.MOD_ID + ".wire.same_connector";
                }
            }
            if (!data.validate(graph, itemData, points.size())) {
                translationKey = "item." + WiresApi.MOD_ID + ".wire.connector_invalid";
            } else {
                break checks;
            } 
            player.displayClientMessage(TextUtils.translate(translationKey, getWireType().getMaxLength()).withStyle(ChatFormatting.RED), true);
            clear(stack);
            return false;
        }

        CompoundTag nodeMeta = new CompoundTag();
        DLUtils.doIfNotNull(metadata, x -> x.accept(customDataNbt, nodeMeta));

        CompoundTag nodeData = data.getRegistryType().wrap(data);
        nodeData.put(NBT_CUSTOM_DATA, nodeMeta);
        points.add(nodeData);
        return true;
    }

    protected boolean canCreateWire(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points) {
        return points.size() >= getRequiredPoints();
    }

    
    protected boolean createWire(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points) {
        WireGraph graph = WireGraphManager.get(level, getWireType().getGraphId(itemData));
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
        graph.createEdge(getWireType(), new CustomData(metaCollection), deserializedData.get(0), deserializedData.get(1), idx);
        clear(stack);
        return true;
    }

    public InteractionResult placeWire(Level level, Player player, InteractionHand hand, HitResult hit, EWireConnectorType type, BiConsumer<CompoundTag, CompoundTag> metadata) { 
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof WireBaseItem)) {
            return InteractionResult.FAIL;
        }

        // --- Decode Item data ---
        CompoundTag itemData = stack.getOrCreateTag();
        CompoundTag customDataNbt = itemData.getCompound(NBT_CUSTOM_DATA);
        List<CompoundTag> points = new ArrayList<>();        
        if (itemData.contains(NBT_POINTS)) {
            points.addAll(itemData.getList(NBT_POINTS, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList());
        }

        // --- Set data ---
        if (!addNewPoint(level, player, hand, hit, type, metadata, stack, itemData, customDataNbt, points)) {
            return InteractionResult.FAIL;
        }

        // --- Save data ---
        ListTag pointsList = new ListTag();
        for (CompoundTag p : points) {
            pointsList.add(p);
        }
        itemData.put(NBT_POINTS, pointsList);
        itemData.put(NBT_CUSTOM_DATA, customDataNbt);
        stack.setTag(itemData);

        // --- Create wire ---
        if (canCreateWire(level, player, hand, hit, stack, itemData, customDataNbt, points)) {
            createWire(level, player, hand, hit, stack, itemData, customDataNbt, points);
        }
        
        return InteractionResult.SUCCESS;
    }

    protected void clear(ItemStack stack) {
        stack.setTag(new CompoundTag());
    }

    protected NodeData createNodeData(Level level, Player player, InteractionHand hand, HitResult hit, EWireConnectorType type) {
        if (hit instanceof BlockHitResult blockHit) {
            if (level.getBlockEntity(blockHit.getBlockPos()) instanceof WireConnectorBlockEntity) {
                return new BlockConnectorNodeData(blockHit.getBlockPos());
            }
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = level.getBlockState(blockHit.getBlockPos());
            VoxelShape shape = state.getVisualShape(level, blockHit.getBlockPos(), CollisionContext.empty());
            return clipFromSide(shape, pos, blockHit.getDirection()).map(x -> {
                return new GenericBlockNodeData(blockHit.getBlockPos(), x.getLocation().toVector3f());
            }).orElse(null);
        }
        return null;
    }

    public static Optional<BlockHitResult> clipFromSide(VoxelShape shape, BlockPos pos, Direction dir) {
        if (shape.isEmpty()) {
            return Optional.empty();
        }

        AABB bounds = shape.bounds();
        double minX = 0.5;
        double minY = 0.5;
        double minZ = 0.5;
        double maxX = 0.5;
        double maxY = 0.5;
        double maxZ = 0.5;

        switch (dir) {
            case DOWN  -> {
                minY = bounds.maxY;
                maxY = bounds.minY;
            }
            case UP    -> {
                minY = bounds.minY;
                maxY = bounds.maxY;
            }
            case NORTH -> {
                minZ = bounds.maxZ;
                maxZ = bounds.minZ;
            }
            case SOUTH -> {
                minZ = bounds.minZ;
                maxZ = bounds.maxZ;
            }
            case WEST  -> {
                minX = bounds.maxX;
                maxX = bounds.minX;
            }
            case EAST  -> {
                minX = bounds.minX;
                maxX = bounds.maxX;
            }
        }

        Vec3 block = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        Vec3 start = new Vec3(
                maxX + dir.getStepX() * 0.001,
                maxY + dir.getStepY() * 0.001,
                maxZ + dir.getStepZ() * 0.001
        ).add(block);
        Vec3 end = new Vec3(
                minX - dir.getStepX() * 0.001,
                minY - dir.getStepY() * 0.001,
                minZ - dir.getStepZ() * 0.001
        ).add(block);
        return Optional.ofNullable(shape.clip(start, end, pos));
    }



    
    


    public InteractionResult placeWire24525(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack, Optional<UseOnContext> context, Consumer<CompoundTag> addMetadata) {   
        if (state.getBlock() instanceof IWireConnector wc && wc.canConnectWire(level, pos, state)) {
            if (!level.isClientSide) {
                CompoundTag compound = getTag(stack);
                List<CompoundTag> points = new ArrayList<>(compound.getList(NBT_POINTS, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList());

                CompoundTag pointNbt = new CompoundTag();
                Utils.putNbtBlockPos(pointNbt, NBT_POS, pos);
                pointNbt.putInt(NBT_CONNECTOR_TYPE, EWireConnectorType.CONNECTOR.getId());
                DLUtils.doIfNotNull(addMetadata, x -> x.accept(pointNbt));
                points.add(pointNbt);
                if (!wc.onAttachWireTo(level, pos, state, player, context, pointNbt, points.size())) {                    
                    createEmptyTag(stack);
                    return InteractionResult.SUCCESS;
                }

                ListTag list = new ListTag();
                list.addAll(points);
                compound.put(NBT_POINTS, list);

                int pointsRequired = 2;
                if (pointsRequired < 2) {
                    throw new IllegalArgumentException("At least 2 wire points are required.");
                }

                if (points.size() >= pointsRequired) {                    
                    BlockPos lastPos = null;
                    validation: {
                        List<ConnectionPointData> pointData = new ArrayList<>(points.size());
                        for (CompoundTag point : points) {
                            BlockPos pPos = Utils.getNbtBlockPos(point, NBT_POS);

                            if (lastPos != null && Math.sqrt(lastPos.distSqr(pPos)) > getWireType().getMaxLength()) {
                                player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.to_far_away", getWireType().getMaxLength()).withStyle(ChatFormatting.RED), true);
                                break validation;
                            } else if (!level.isLoaded(pPos)) {
                                player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.connection_not_loaded").withStyle(ChatFormatting.RED), true);
                                break validation;
                            } else if (!(level.getBlockState(pPos).getBlock() instanceof IWireConnector)) {
                                player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.connector_invalid").withStyle(ChatFormatting.RED), true);
                                break validation;
                            } else if (lastPos != null && lastPos.equals(pPos)) {
                                player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.same_connector").withStyle(ChatFormatting.RED), true);
                                break validation;
                            }
                            pointData.add(new ConnectionPointData(pPos, (IWireConnector)level.getBlockState(pPos).getBlock()));
                            lastPos = pPos;
                        }
                        
                        // Do it...
                        for (int i = 1; i < pointData.size(); i++) {
                            final int k = i;
                            ConnectionPointData pointA = pointData.get(k - 1);
                            ConnectionPointData pointB = pointData.get(k);

                            if (!wc.beforeCreateWireConnection(level, pos, state, player, context, compound, k)) {
                                break;
                            }
                            if (true) {
                                if (!wc.afterCreateWireConnection(level, pos, state, player, context, compound, k)) {
                                    break;
                                }
                            } else {
                                // Cannot create connection, because it already exists
                                player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.connection_already_exists").withStyle(ChatFormatting.RED), true);
                            }
                        }
                    }
                    
                    createEmptyTag(stack);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private CompoundTag createEmptyTag(ItemStack stack) {
        CompoundTag nbt = new CompoundTag();
        nbt.put(NBT_POINTS, new ListTag());
        stack.setTag(nbt);
        return nbt;
    }

    protected final CompoundTag getTag(ItemStack stack) {
        CompoundTag nbt = stack.getOrCreateTag();
        if (!nbt.contains(NBT_POINTS)) nbt.put(NBT_POINTS, new ListTag());
        return nbt;
    }

    /**
     * The text rendered above the hotbar when creating a wire connection which displays information, such as the first connection point and the current distance to that point.
     * @param stack The itemstack
     * @param hit The current location the player is looking at
     * @return The text component which is displayed in the HUD
     */
    public Component createHudInfoText(ItemStack stack, Player player, HitResult hit) {
        if (!stack.hasTag()) {
            return null;
        }
        
        CompoundTag itemData = getTag(stack);
        ListTag list = itemData.getList(NBT_POINTS, Tag.TAG_COMPOUND);
        WireGraph graph = WireGraphManager.get(player.level(), getWireType().getGraphId(itemData));
        if (graph == null || list.isEmpty()) {
            return null;
        }

        CompoundTag lastPointData = (CompoundTag)list.get(list.size() - 1);
        NodeData node = WiresApi.NODE_DATA_REGISTRY.load(lastPointData);
        Vector3f pos = node.toWorldPos(graph);

        int maxLength = getWireType().getMaxLength();
        int distance;
        if (hit instanceof BlockHitResult r) {
            distance = (int)pos.distance(r.getLocation().toVector3f());
        } else {
            distance = (int)pos.distance(player.getEyePosition().toVector3f());
        }
        return TextUtils.empty().withStyle(ChatFormatting.WHITE)
            .append(TextUtils.text(String.format("X: %s, Y: %s, Z: %s", (int)pos.x(), (int)pos.y(), (int)pos.z())).withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(" \u25A0 ").withStyle(ChatFormatting.GRAY))
            .append(TextUtils.text(String.format("%sm / %sm", (int)distance, getWireType().getMaxLength())).withStyle(distance == maxLength ? ChatFormatting.GOLD : (distance < maxLength ? ChatFormatting.GREEN : ChatFormatting.RED)))
        ;
    }

    
}
