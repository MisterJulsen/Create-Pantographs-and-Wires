package de.mrjulsen.paw.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import de.mrjulsen.paw.components.WireConnectionDataComponent;
import de.mrjulsen.paw.registry.ModBlockTags;
import de.mrjulsen.paw.registry.ModDataComponents;
import org.joml.Vector3d;

import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.data.WireHitResult;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.data.node.LatticeMastNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.registry.DLStaticRegistryObject;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphClient;
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
        if (hit instanceof BlockHitResult h && level.getBlockState(h.getBlockPos()).getTags().anyMatch(x -> x.equals(ModBlockTags.CATENARY_HEADSPAN_CONNECTABLE))) {
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
        WireConnectionDataComponent connectionData = ModDataComponents.getComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA, WireConnectionDataComponent::empty);
        CompoundTag customDataNbt = connectionData.customData();
        List<CompoundTag> points = new ArrayList<>(connectionData.customPointData());

        // --- Set data ---
        if (points.size() < 2) {
            if (!addNewPoint(level, player, hand, hit, metadata, stack, connectionData, points)) {
                clear(stack);
                return InteractionResult.FAIL;
            }
        } else if (!customDataNbt.contains(NBT_UPPER_WIRE_HEIGHT) || !customDataNbt.contains(NBT_TOP_WIRE_HEIGHT)) {
            CompoundTag startPointData = points.get(0);
            CompoundTag endPointData = points.get(1);
            NodeData nodeA = WiresApi.NODE_DATA_REGISTRY.load(startPointData);
            NodeData nodeB = WiresApi.NODE_DATA_REGISTRY.load(endPointData);

            if (nodeA instanceof LatticeMastNodeData nA && nodeB instanceof LatticeMastNodeData nB) {                
                if (hit instanceof BlockHitResult h && level.getBlockState(h.getBlockPos()).getTags().anyMatch(x -> x.equals(ModBlockTags.CATENARY_HEADSPAN_CONNECTABLE))) {
                    if (!customDataNbt.contains(NBT_UPPER_WIRE_HEIGHT)) {
                        int min = ModServerConfig.CATENARY_HEADSPAN_MIN_UPPER_TENSION_WIRE.get();
                        int max = ModServerConfig.CATENARY_HEADSPAN_MAX_UPPER_TENSION_WIRE.get();
                        float d = h.getBlockPos().getY() - nB.getBlockPos().getY();
                        if (d < min) { 
                            player.displayClientMessage(TextUtils.translate(KEY_HEIGHT_DIFFERENCE_TOO_SMALL, min, max).withStyle(ChatFormatting.RED), true);
                            clear(stack);
                            return InteractionResult.FAIL;
                        } else if (d > max) { 
                            player.displayClientMessage(TextUtils.translate(KEY_HEIGHT_DIFFERENCE_TOO_LARGE, min, max).withStyle(ChatFormatting.RED), true);
                            clear(stack);
                            return InteractionResult.FAIL;
                        }
                        customDataNbt.putFloat(NBT_UPPER_WIRE_HEIGHT, d);
                    } else if (!customDataNbt.contains(NBT_TOP_WIRE_HEIGHT)) {
                        double p = customDataNbt.getFloat(NBT_UPPER_WIRE_HEIGHT);
                        double min = p + calcSupportWireMinHeightDifference(new Vector3d(nA.getBlockPos().getX(), nA.getBlockPos().getY(), nA.getBlockPos().getZ()), new Vector3d(nB.getBlockPos().getX(), nB.getBlockPos().getY(), nB.getBlockPos().getZ()));
                        double max = min + ModServerConfig.CATENARY_HEADSPAN_MAX_TOP_SUPPORT_WIRE.get();
                        double d = h.getBlockPos().getY() - nB.getBlockPos().getY() - p;
                        if (d < min) { 
                            player.displayClientMessage(TextUtils.translate(KEY_HEIGHT_DIFFERENCE_TOO_SMALL, min, max).withStyle(ChatFormatting.RED), true);
                            clear(stack);
                            return InteractionResult.FAIL;
                        } else if (d > max) { 
                            player.displayClientMessage(TextUtils.translate(KEY_HEIGHT_DIFFERENCE_TOO_LARGE, min, max).withStyle(ChatFormatting.RED), true);
                            clear(stack);
                            return InteractionResult.FAIL;
                        }
                        customDataNbt.putDouble(NBT_TOP_WIRE_HEIGHT, d); // TODO was float
                    }
                }
            }
        }

        // --- Save data ---
        connectionData = new WireConnectionDataComponent(points, customDataNbt);
        ModDataComponents.setComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA, connectionData);

        // --- Create wire ---
        if (canCreateWire(level, player, hand, hit, stack, connectionData, points)) {
            WireGraph.CreateEdgeResult result = createWire(level, player, hand, hit, stack, connectionData, points);
            if (result.success()) {
                removeWireItem(level, player, hand, hit, stack, result.edge().get().length());
            }
        }
        
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canCreateWire(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, WireConnectionDataComponent connectionData, List<CompoundTag> points) {
        return IPawWireItemBase.super.canCreateWire(level, player, hand, hit, stack, connectionData, points) && connectionData.customData().contains(NBT_UPPER_WIRE_HEIGHT) && connectionData.customData().contains(NBT_TOP_WIRE_HEIGHT);
    }

    @Override
    public Component createHudInfoText(ItemStack stack, Player player, HitResult hit) {
        if (!ModDataComponents.hasComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA)) {
            return null;
        }

        WireConnectionDataComponent connectionData = ModDataComponents.getComponent(stack, ModDataComponents.WIRE_CONNECTION_DATA, WireConnectionDataComponent::empty);
        List<CompoundTag> points = new ArrayList<>(connectionData.customPointData());
        if (points.size() < 2) {
            return IPawWireItemBase.super.createHudInfoText(stack, player, hit);
        }

        WireGraphClient graph = WireGraphManager.getClient(player.level(), getWireType(stack).getGraphId(connectionData));
        if (graph == null) {
            return null;
        }


        CompoundTag customDataNbt = connectionData.customData();
        CompoundTag startPointData = points.getFirst();
        CompoundTag endPointData = points.get(1);
        NodeData nodeA = WiresApi.NODE_DATA_REGISTRY.load(startPointData);
        NodeData nodeB = WiresApi.NODE_DATA_REGISTRY.load(endPointData);
        Vector3d pos = nodeB.toWorldPos(graph);
        Vector3d targetPos;
        if (hit instanceof BlockHitResult r) {
            targetPos = new Vector3d(r.getLocation().x(), r.getLocation().y(), r.getLocation().z());
        } else {
            targetPos = new Vector3d(player.getEyePosition().x(), player.getEyePosition().y(), player.getEyePosition().z());
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
            double min = customDataNbt.getFloat(NBT_UPPER_WIRE_HEIGHT) + calcSupportWireMinHeightDifference(nodeA.toWorldPos(graph), nodeB.toWorldPos(graph));
            double max = min + ModServerConfig.CATENARY_HEADSPAN_MAX_TOP_SUPPORT_WIRE.get();
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

    public static int calcSupportWireMinHeightDifference(Vector3d a, Vector3d b) {
        return (int)(Math.floor(a.distance(b) / 16f));
    }
}
