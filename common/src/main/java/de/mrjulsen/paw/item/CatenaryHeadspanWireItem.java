package de.mrjulsen.paw.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.data.node.LatticeMastNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.registry.DLStaticRegistryObject;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.item.IWireItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CatenaryHeadspanWireItem implements IPawWireItemBase {
    
    public static final String NBT_UPPER_WIRE_HEIGHT = "UpperWireHeight";
    public static final String NBT_TOP_WIRE_HEIGHT = "TopWireHeight";
    
	private static final String KEY_HEIGHT_DIFFERENCE_TOO_SMALL = "item." + PantographsAndWires.MOD_ID + ".catenary_headspan.small_height_difference";
	private static final String KEY_HEIGHT_DIFFERENCE_TOO_LARGE = "item." + PantographsAndWires.MOD_ID + ".catenary_headspan.large_height_difference";


    @Override
    public InteractionResult interactWithWire(Level level, Player player, InteractionHand hand, WireHitResult hit) {
        return placeWire(level, player, hand, hit, (a, b) -> {});
    }

    @Override
    public IWireType getWireType(ItemStack stack) {
        return ModWireRegistry.CATENARY_HEADSPAN;
    }    

    @Override
    public DLStaticRegistryObject<IPawWireItemBase> getRegistryType() {
        return (DLStaticRegistryObject<IPawWireItemBase>)(Object)ModWireRegistry.CATENARY_HEADSPAN_ITEM_SUBTYPE;
    }  

    @Override
    public String getTranslationKey() {
        return "wire." + PantographsAndWires.MOD_ID + ".catenary_headspan";
    }

    @Override
    public ModGuiIcons getIcon() {
        return ModGuiIcons.CATENARY_HEADSPAN_WIRE;
    }

    @Override
    public NodeData createNodeData(Level level, Player player, InteractionHand hand, HitResult hit) {
        if (hit instanceof BlockHitResult h && level.getBlockState(h.getBlockPos()).getTags().anyMatch(x -> x.equals(ModBlocks.TAG_CATENARY_HEADSPAN_CONNECTABLE))) {
            return new LatticeMastNodeData(h.getBlockPos());
        }
        return null;
    }

    @Override
    public InteractionResult placeWire(Level level, Player player, InteractionHand hand, HitResult hit, BiConsumer<CompoundTag, CompoundTag> metadata) { 
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof IWireItemBase)) {
            return InteractionResult.FAIL;
        }

        // --- Decode Item data ---
        CompoundTag itemData = IWireItemBase.getNbt(stack);
        CompoundTag customDataNbt = itemData.getCompound(NBT_CUSTOM_DATA);
        List<CompoundTag> points = new ArrayList<>();        
        if (itemData.contains(NBT_POINTS)) {
            points.addAll(itemData.getList(NBT_POINTS, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList());
        }

        // --- Set data ---
        if (points.size() < 2) {
            if (!addNewPoint(level, player, hand, hit, metadata, stack, itemData, customDataNbt, points)) {
                IWireItemBase.clear(stack);
                return InteractionResult.FAIL;
            }
        } else if (!customDataNbt.contains(NBT_UPPER_WIRE_HEIGHT) || !customDataNbt.contains(NBT_TOP_WIRE_HEIGHT)) {
            CompoundTag startPointData = (CompoundTag)points.get(0);
            CompoundTag endPointData = (CompoundTag)points.get(1);
            NodeData nodeA = WiresApi.NODE_DATA_REGISTRY.load(startPointData);
            NodeData nodeB = WiresApi.NODE_DATA_REGISTRY.load(endPointData);

            if (nodeA instanceof LatticeMastNodeData nA && nodeB instanceof LatticeMastNodeData nB) {                
                if (hit instanceof BlockHitResult h && level.getBlockState(h.getBlockPos()).getTags().anyMatch(x -> x.equals(ModBlocks.TAG_CATENARY_HEADSPAN_CONNECTABLE))) {
                    if (!customDataNbt.contains(NBT_UPPER_WIRE_HEIGHT)) {
                        int min = ModServerConfig.CATENARY_HEADSPAN_MIN_UPPER_TENSION_WIRE.get();
                        int max = ModServerConfig.CATENARY_HEADSPAN_MAX_UPPER_TENSION_WIRE.get();
                        float d = h.getBlockPos().getY() - nB.getBlockPos().getY();
                        if (d < min) { 
                            player.displayClientMessage(TextUtils.translate(KEY_HEIGHT_DIFFERENCE_TOO_SMALL, min, max).withStyle(ChatFormatting.RED), true);
                            IWireItemBase.clear(stack);
                            return InteractionResult.FAIL;
                        } else if (d > max) { 
                            player.displayClientMessage(TextUtils.translate(KEY_HEIGHT_DIFFERENCE_TOO_LARGE, min, max).withStyle(ChatFormatting.RED), true);
                            IWireItemBase.clear(stack);
                            return InteractionResult.FAIL;
                        }
                        customDataNbt.putFloat(NBT_UPPER_WIRE_HEIGHT, d);
                    } else if (!customDataNbt.contains(NBT_TOP_WIRE_HEIGHT)) {
                        float p = customDataNbt.getFloat(NBT_UPPER_WIRE_HEIGHT);
                        float min = p + calcSupportWireMinHeightDifference(new Vector3f(nA.getBlockPos().getX(), nA.getBlockPos().getY(), nA.getBlockPos().getZ()), new Vector3f(nB.getBlockPos().getX(), nB.getBlockPos().getY(), nB.getBlockPos().getZ()));
                        float max = min + ModServerConfig.CATENARY_HEADSPAN_MAX_TOP_SUPPORT_WIRE.get();
                        float d = h.getBlockPos().getY() - nB.getBlockPos().getY() - p;                        
                        if (d < min) { 
                            player.displayClientMessage(TextUtils.translate(KEY_HEIGHT_DIFFERENCE_TOO_SMALL, min, max).withStyle(ChatFormatting.RED), true);
                            IWireItemBase.clear(stack);
                            return InteractionResult.FAIL;
                        } else if (d > max) { 
                            player.displayClientMessage(TextUtils.translate(KEY_HEIGHT_DIFFERENCE_TOO_LARGE, min, max).withStyle(ChatFormatting.RED), true);
                            IWireItemBase.clear(stack);
                            return InteractionResult.FAIL;
                        }
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
        IWireItemBase.setNbt(stack, itemData);

        // --- Create wire ---
        if (canCreateWire(level, player, hand, hit, stack, itemData, customDataNbt, points)) {
            createWire(level, player, hand, hit, stack, itemData, customDataNbt, points);
        }
        
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canCreateWire(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, CompoundTag itemData, CompoundTag customDataNbt, List<CompoundTag> points) {
        return IPawWireItemBase.super.canCreateWire(level, player, hand, hit, stack, itemData, customDataNbt, points) && customDataNbt.contains(NBT_UPPER_WIRE_HEIGHT) && customDataNbt.contains(NBT_TOP_WIRE_HEIGHT);
    }

    @Override
    public Component createHudInfoText(ItemStack stack, Player player, HitResult hit) {
        if (!stack.hasTag()) {
            return null;
        }
        
        CompoundTag itemData = IWireItemBase.getNbt(stack);
        ListTag list = itemData.getList(NBT_POINTS, Tag.TAG_COMPOUND);
        if (list.size() < 2) {
            return IPawWireItemBase.super.createHudInfoText(stack, player, hit);
        }

        WireGraph graph = WireGraphManager.get(player.level(), getWireType(stack).getGraphId(itemData));
        if (graph == null || list.isEmpty()) {
            return null;
        }

        

        CompoundTag customDataNbt = itemData.getCompound(NBT_CUSTOM_DATA);
        CompoundTag startPointData = (CompoundTag)list.get(0);
        CompoundTag endPointData = (CompoundTag)list.get(1);
        NodeData nodeA = WiresApi.NODE_DATA_REGISTRY.load(startPointData);
        NodeData nodeB = WiresApi.NODE_DATA_REGISTRY.load(endPointData);
        Vector3f pos = nodeB.toWorldPos(graph);
        Vector3f targetPos;
        if (hit instanceof BlockHitResult r) {
            targetPos = r.getLocation().toVector3f();
        } else {
            targetPos = player.getEyePosition().toVector3f();
        }

        if (!customDataNbt.contains(NBT_UPPER_WIRE_HEIGHT)) {
            int min = ModServerConfig.CATENARY_HEADSPAN_MIN_UPPER_TENSION_WIRE.get();
            int max = ModServerConfig.CATENARY_HEADSPAN_MAX_UPPER_TENSION_WIRE.get();
            int diff = (int)Math.floor(targetPos.y()) - (int)pos.y();
            return TextUtils.empty().withStyle(ChatFormatting.WHITE)
                .append(TextUtils.text(String.format("Y: %s", (int)pos.y())).withStyle(ChatFormatting.WHITE))
                .append(TextUtils.text(" \u25A0 ").withStyle(ChatFormatting.GRAY))
                .append(TextUtils.text("Upper Tension Wire height difference").withStyle(ChatFormatting.WHITE))
                .append(TextUtils.text(" \u25A0 ").withStyle(ChatFormatting.GRAY))
                .append(TextUtils.text(String.format("%sm [%sm - %sm]", diff, min, max)).withStyle(diff < min || diff > max ? ChatFormatting.RED : ChatFormatting.GREEN))
            ;
        } else if (!customDataNbt.contains(NBT_TOP_WIRE_HEIGHT)) {
            float min = customDataNbt.getFloat(NBT_UPPER_WIRE_HEIGHT) + calcSupportWireMinHeightDifference(nodeA.toWorldPos(graph), nodeB.toWorldPos(graph));
            float max = min + ModServerConfig.CATENARY_HEADSPAN_MAX_TOP_SUPPORT_WIRE.get();
            int diff = (int)(Math.floor(targetPos.y()) - pos.y() - min);
            return TextUtils.empty().withStyle(ChatFormatting.WHITE)
                .append(TextUtils.text(String.format("Y: %s", (int)pos.y())).withStyle(ChatFormatting.WHITE))
                .append(TextUtils.text(" \u25A0 ").withStyle(ChatFormatting.GRAY))
                .append(TextUtils.text("Top Support Wire height difference").withStyle(ChatFormatting.WHITE))
                .append(TextUtils.text(" \u25A0 ").withStyle(ChatFormatting.GRAY))
                .append(TextUtils.text(String.format("%sm [%sm - %sm]", diff, min, max)).withStyle(diff < min || diff > max ? ChatFormatting.RED : ChatFormatting.GREEN))
            ;
        }

        return TextUtils.empty();
    }

    public static int calcSupportWireMinHeightDifference(Vector3f a, Vector3f b) {
        return (int)(Math.floor(a.distance(b) / 16f));
    }
}
