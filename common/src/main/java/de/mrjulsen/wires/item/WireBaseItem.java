package de.mrjulsen.wires.item;

import de.mrjulsen.wires.util.Utils;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WireNetwork;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

    public static final String NBT_POINT_A = "PointA";
    public static final String NBT_POINT_B = "PointB";
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

        if (state.getBlock() instanceof IWireConnector wc && getWireType().isValidConnector(level, pos, wc) && wc.canConnectWire(level, pos, state)) {
            if (!level.isClientSide) {
                CompoundTag compound = context.getItemInHand().getOrCreateTag();
                if (!compound.contains(NBT_POINT_A)) {
                    Utils.putNbtBlockPos(compound, NBT_POINT_A, pos);
                    wc.onPlaceWireOn(level, pos, state, player, context, compound, 1);
                } else {
                    Utils.putNbtBlockPos(compound, NBT_POINT_B, pos);
                    wc.onPlaceWireOn(level, pos, state, player, context, compound, 2);
                    final BlockPos posA = Utils.getNbtBlockPos(compound, NBT_POINT_A);
                    final BlockPos posB = Utils.getNbtBlockPos(compound, NBT_POINT_B);
                    
                    if (Math.sqrt(posA.distSqr(posB)) > getWireType().getMaxLength()) {
                        player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.to_far_away", getWireType().getMaxLength()).withStyle(ChatFormatting.RED), true);
                    } else if (!level.isLoaded(posA) || !level.isLoaded(posB)) {
                        // Check if loaded
                        player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.connection_not_loaded").withStyle(ChatFormatting.RED), true);
                    } else if (!(level.getBlockState(posA).getBlock() instanceof IWireConnector wcA) || !(level.getBlockState(posB).getBlock() instanceof IWireConnector wcB)) {
                        // Check if connectors are still valid
                        player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.connector_invalid").withStyle(ChatFormatting.RED), true);
                    } else if (posA.equals(posB)) {
                        // Check if same position
                        player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.same_connector").withStyle(ChatFormatting.RED), true);
                    } else {
                        // Do it...
                        wc.beforeCreateWireConnection(level, pos, state, player, context, compound);
                        if (WireNetwork.get(level).addConnection(level, compound, posA, posB, wcA, wcB, getWireType())) {
                            wc.afterCreateWireConnection(level, pos, state, player, context, compound);
                        } else {
                            // Cannot create connection, because it already exists
                            player.displayClientMessage(TextUtils.translate("item." + WiresApi.MOD_ID + ".wire.connection_already_exists").withStyle(ChatFormatting.RED), true);
                        }
                    }
                    context.getItemInHand().setTag(new CompoundTag());
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
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
        BlockPos pos = Utils.getNbtBlockPos(stack.getOrCreateTag(), NBT_POINT_A);
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
