package de.mrjulsen.wires.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiConsumer;

import de.mrjulsen.paw.components.WireAmountComponent;
import de.mrjulsen.paw.components.WireConnectionDataComponent;
import de.mrjulsen.paw.registry.ModDataComponents;
import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3d;

import de.mrjulsen.mcdragonlib.data.DLStatus;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.client.gui.widgets.IIconRepresentable;
import de.mrjulsen.paw.client.gui.widgets.ITranslatable;
import de.mrjulsen.paw.item.PAWWireType;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.registry.IStaticRegisterable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;


public interface IPawWireItemBase extends IWireItemBase, IStaticRegisterable<IPawWireItemBase>, IIconRepresentable, ITranslatable {

    @Override
    default void removeWireItem(Level level, Player player, InteractionHand hand, HitResult hit, ItemStack stack, int length) {
        if (!player.isCreative() && !player.isSpectator() && stack.getItem() instanceof MultiWireItem) {
            if (getWireType(stack) instanceof PAWWireType paw) {
                length = (int)((double)length * paw.getWireConsumptionMultiplier(length));
            }
            updateWireAmount(player, stack, -length);
        }
    }

    @Override
    default DLStatus testPoint(Level level, Player player, InteractionHand hand, HitResult hit, BiConsumer<CompoundTag, CompoundTag> metadata, ItemStack stack, WireConnectionDataComponent connectionData, List<CompoundTag> points, NodeData nodeData) {
        DLStatus result = IWireItemBase.super.testPoint(level, player, hand, hit, metadata, stack, connectionData, points, nodeData);
        if (result.flag() != DLStatus.FLAG_OK) {
            return result;
        }

        WireGraph graph = WireGraphManager.get(level, getWireType(stack).getGraphId(connectionData));
        if (!points.isEmpty()) {
            NodeData previousNode = WiresApi.NODE_DATA_REGISTRY.load(points.get(points.size() - 1));
            int distance = (int)previousNode.toWorldPos(graph).distance(nodeData.toWorldPos(graph));
            if (getWireType(stack) instanceof PAWWireType paw && (double)distance * paw.getWireConsumptionMultiplier(distance) > getRemainingWire(stack)) {
                return new DLStatus(DLStatus.FLAG_ERROR, 0, "item." + PantographsAndWires.MOD_ID + ".wire.not_enough_wire");
            }
        }
        return result;
    }
    
    public static int getRemainingWire(ItemStack stack) {
        return ModDataComponents.getOrSetComponent(stack, ModDataComponents.WIRE_AMOUNT, WireAmountComponent::empty).wireAmount();
    }
    
    
    default int getContextRemainingWire(ItemStack stack) {
        int remaining = getRemainingWire(stack);
        if (getWireType(stack) instanceof PAWWireType paw) {
            remaining = (int)((double)remaining / paw.getWireConsumptionMultiplier(remaining));
        }
        return remaining;
    }

    /**
     * 
     * @param stack
     * @param length
     * @return The amount of wire that has fallen below or exceeded the limit.
     */
    public static int updateWireAmount(Player player, ItemStack stack, int length) {
        int current = ModDataComponents.getOrSetComponent(stack, ModDataComponents.WIRE_AMOUNT, WireAmountComponent::empty).wireAmount();
        int newValue = current + length;
        int cleanValue = MathUtils.clamp(newValue, 0, WireAmountComponent.MAX_WIRE);
        if (newValue <= 0) {
            if (player == null || (!player.isCreative() && !player.isSpectator())) {
                stack.shrink(1);
            }
            player.getInventory().add(ModItems.EMPTY_WIRE_COIL.asStack());
        } else {
            ModDataComponents.setComponent(stack, ModDataComponents.WIRE_AMOUNT, new WireAmountComponent(cleanValue));
        }
        return newValue - cleanValue;
    }

    public static void setWireAmount(Player player, ItemStack stack, int newValue) {
        if (newValue <= 0) {
            if (player == null || (!player.isCreative() && !player.isSpectator())) {
                stack.shrink(1);
            }
            player.getInventory().add(ModItems.EMPTY_WIRE_COIL.asStack());
        } else {
            ModDataComponents.setComponent(stack, ModDataComponents.WIRE_AMOUNT, new WireAmountComponent(newValue));
        }
    }

    @Override
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
        int availableLength = getContextRemainingWire(stack);
        int distance;
        if (hit instanceof BlockHitResult r) {
            distance = (int)pos.distance(new Vector3d(r.getLocation().x(), r.getLocation().y(), r.getLocation().z()));
        } else {
            distance = (int)pos.distance(new Vector3d(player.getEyePosition().x(), player.getEyePosition().y(), player.getEyePosition().z()));
        }
        MutableComponent text = TextUtils.empty().withStyle(ChatFormatting.WHITE)
            .append(TextUtils.text(String.format("X: %s, Y: %s, Z: %s", (int)pos.x(), (int)pos.y(), (int)pos.z())).withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(" \u25A0 ").withStyle(ChatFormatting.GRAY));

            if (availableLength < maxLength) {
                int ml = Math.min(maxLength, availableLength);
                text = text
                    .append(TextUtils.text(String.format("%sm / ", distance)).withStyle(distance == ml ? ChatFormatting.GOLD : (distance < ml ? ChatFormatting.GREEN : ChatFormatting.RED)))
                    .append(TextUtils.text(String.format("%sm", maxLength)).withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.STRIKETHROUGH))
                    .append(TextUtils.text(String.format(" %sm", availableLength)).withStyle(distance == ml ? ChatFormatting.GOLD : (distance < ml ? ChatFormatting.GREEN : ChatFormatting.RED)));
            } else {
                text = text.append(String.format("%sm / %sm", distance, getWireType(stack).getMaxLength())).withStyle(distance == maxLength ? ChatFormatting.GOLD : (distance < maxLength ? ChatFormatting.GREEN : ChatFormatting.RED));
            }

            return text;
    }

    public static boolean creditWireToInventory(Player player, int amount) {
        MutableInt remaining = new MutableInt(amount);

        int inventorySize = player.getInventory().getContainerSize();
        Queue<ItemStack> wireCoilStacks = new LinkedBlockingDeque<>(inventorySize);
        int freeSlots = 0;
        int availableEmptyCoils = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                freeSlots++;
            } else if (ModItems.EMPTY_WIRE_COIL.is(stack.getItem())) {
                availableEmptyCoils += stack.getCount();
            } else if (ModItems.WIRE.is(stack.getItem()) && IPawWireItemBase.getRemainingWire(stack) < WireAmountComponent.MAX_WIRE) {
                wireCoilStacks.add(stack);
            }
        }

        boolean mainHandFull = IPawWireItemBase.getRemainingWire(player.getMainHandItem()) >= WireAmountComponent.MAX_WIRE;
        boolean offhandFull = IPawWireItemBase.getRemainingWire(player.getOffhandItem()) >= WireAmountComponent.MAX_WIRE;
        while (remaining.intValue() > 0 && (!mainHandFull || !offhandFull || !wireCoilStacks.isEmpty() || (availableEmptyCoils > 0 && freeSlots > 0))) {
            if (!mainHandFull) {
                ItemStack stack = player.getMainHandItem();
                int d = IPawWireItemBase.updateWireAmount(player, stack, remaining.intValue());
                remaining.setValue(d);
                mainHandFull |= d > 0;
            } else if (!offhandFull) {
                ItemStack stack = player.getOffhandItem();
                int d = IPawWireItemBase.updateWireAmount(player, stack, remaining.intValue());
                remaining.setValue(d);
                offhandFull |= d > 0;
            } else if (!wireCoilStacks.isEmpty()) {
                ItemStack stack = wireCoilStacks.peek();
                int d = IPawWireItemBase.updateWireAmount(player, stack, remaining.intValue());
                remaining.setValue(d);
                wireCoilStacks.remove();
            } else {
                ItemStack stack = ModItems.WIRE.asStack();
                int a = Math.min(remaining.intValue(), WireAmountComponent.MAX_WIRE);
                IPawWireItemBase.setWireAmount(player, stack, a);
                remaining.subtract(a);
                player.getInventory().add(stack);
                player.getInventory().clearOrCountMatchingItems(x -> x.is(ModItems.EMPTY_WIRE_COIL.get()), 1, player.inventoryMenu.getCraftSlots());
                freeSlots--;
                availableEmptyCoils--;
            }
        }
        return remaining.intValue() <= 0;
    }
}
