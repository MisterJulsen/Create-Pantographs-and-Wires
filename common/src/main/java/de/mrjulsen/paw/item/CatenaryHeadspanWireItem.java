package de.mrjulsen.paw.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.data.node.LatticeMastNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.data.node.CatenaryWireConnectorNodeData;
import de.mrjulsen.wires.item.WireBaseItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CatenaryHeadspanWireItem extends WireBaseItem {

    public static final String NBT_UPPER_WIRE_HEIGHT = "UpperWireHeight";
    public static final String NBT_TOP_WIRE_HEIGHT = "TopWireHeight";

    public CatenaryHeadspanWireItem(Properties properties, IWireType wireType) {
        super(properties, wireType);
    }


    @Override
    public InteractionResult interactWithWire(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        return placeWire(level, player, hand, hit, EWireConnectorType.WIRE, (a, b) -> {});
    }
    

    @Override
    protected NodeData createNodeData(Level level, Player player, InteractionHand hand, HitResult hit, EWireConnectorType type) {
        if (hit instanceof BlockHitResult h && level.getBlockState(h.getBlockPos()).getTags().anyMatch(x -> x.equals(ModBlocks.TAG_CATENARY_HEADSPAN_CONNECTABLE))) {
            return new LatticeMastNodeData(h.getBlockPos());
        } else if (hit instanceof WireHitResult h) {
            float posOnWire = (float)ModMath.snap(h.getPosOnWire(), 0.5f);
            float p = h.getCollision(level).map(x -> MathUtils.clamp(1F / x.length(h.getWireId().name()) * posOnWire, 0F, 1F)).orElse(0F);
            return new CatenaryWireConnectorNodeData(h.getWireId(), p);
        }
        return null;
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
        if (points.size() < 2) {
            if (!addNewPoint(level, player, hand, hit, type, metadata, stack, itemData, customDataNbt, points)) {
                return InteractionResult.FAIL;
            }
        } else if (!customDataNbt.contains(NBT_UPPER_WIRE_HEIGHT) || !customDataNbt.contains(NBT_TOP_WIRE_HEIGHT)) {
            NodeData previousNode = WiresApi.NODE_DATA_REGISTRY.load(points.get(points.size() - 1));
            if (previousNode instanceof LatticeMastNodeData n) {                
                if (hit instanceof BlockHitResult h && level.getBlockState(h.getBlockPos()).getTags().anyMatch(x -> x.equals(ModBlocks.TAG_CATENARY_HEADSPAN_CONNECTABLE))) {
                    if (!customDataNbt.contains(NBT_UPPER_WIRE_HEIGHT)) {                        
                        float d = h.getBlockPos().getY() - n.getBlockPos().getY();
                        customDataNbt.putFloat(NBT_UPPER_WIRE_HEIGHT, d);
                    } else if (!customDataNbt.contains(NBT_TOP_WIRE_HEIGHT)) {                        
                        float d = h.getBlockPos().getY() - n.getBlockPos().getY();
                        customDataNbt.putFloat(NBT_TOP_WIRE_HEIGHT, d);
                    }
                }
            }
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

    @Override
    protected boolean canCreateWire(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points) {
        return super.canCreateWire(level, player, hand, hit, stack, itemData, customDataNbt, points) && customDataNbt.contains(NBT_UPPER_WIRE_HEIGHT) && customDataNbt.contains(NBT_TOP_WIRE_HEIGHT);
    }
}
