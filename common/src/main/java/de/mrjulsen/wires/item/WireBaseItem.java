package de.mrjulsen.wires.item;

import de.mrjulsen.wires.util.Utils;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WireNetwork;
import de.mrjulsen.wires.WiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Basic item class for a specific {@code IWireType} with basic functionality.
 */
public class WireBaseItem extends Item {
    
    private record ConnectionPointData(BlockPos pos, IWireConnector connector) {}

    public static final String NBT_POINTS = "StoredPoints";
    public static final String NBT_POS = "Pos";

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
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        return placeWire(level, pos, state, player, context.getItemInHand(), Optional.of(context), null);
    }

    public InteractionResult placeWire(Level level, BlockPos pos, BlockState state, Player player, ItemStack stack, Optional<UseOnContext> context, Consumer<CompoundTag> addMetadata) {   
        if (state.getBlock() instanceof IWireConnector wc && getWireType().isValidConnector(level, pos, wc) && wc.canConnectWire(level, pos, state)) {
            if (!level.isClientSide) {
                CompoundTag compound = getTag(stack);
                List<CompoundTag> points = new ArrayList<>(compound.getList(NBT_POINTS, Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList());

                CompoundTag pointNbt = new CompoundTag();
                Utils.putNbtBlockPos(pointNbt, NBT_POS, pos);
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
                            if (WireNetwork.get(level).addConnection(level, compound, pointA.pos(), pointB.pos(), pointA.connector(), pointB.connector(), getWireType(), 0)) {
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

    private CompoundTag getTag(ItemStack stack) {
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
        
        ListTag list = getTag(stack).getList(NBT_POINTS, Tag.TAG_COMPOUND);
        if (list.size() <= 0) {
            return null;
        }
        BlockPos pos = list.stream().map(x -> (CompoundTag)x).findFirst().map(x -> Utils.getNbtBlockPos(x, NBT_POS)).orElse(BlockPos.ZERO);

        int maxLength = getWireType().getMaxLength();
        int distance;
        if (hit instanceof BlockHitResult r) {            
            distance = (int)Math.sqrt(r.getBlockPos().distSqr(pos));
        } else {
            distance = (int)Math.sqrt(player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
        }
        return TextUtils.empty().withStyle(ChatFormatting.WHITE)
            .append(TextUtils.text(String.format("X: %s, Y: %s, Z: %s", pos.getX(), pos.getY(), pos.getZ())).withStyle(ChatFormatting.WHITE))
            .append(TextUtils.text(" \u25A0 ").withStyle(ChatFormatting.GRAY))
            .append(TextUtils.text(String.format("%sm / %sm", (int)distance, getWireType().getMaxLength())).withStyle(distance == maxLength ? ChatFormatting.GOLD : (distance < maxLength ? ChatFormatting.GREEN : ChatFormatting.RED)))
        ;
    }

    
}
