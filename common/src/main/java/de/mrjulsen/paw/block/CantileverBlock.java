package de.mrjulsen.paw.block;

import java.util.Optional;

import javax.annotation.Nullable;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity.CantileverData;
import de.mrjulsen.paw.item.CantileverBlockItem;
import de.mrjulsen.paw.item.CatenaryWireItem;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.wires.item.CustomData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CantileverBlock extends AbstractCantileverBlock {

    private final EInsulatorType insulatorType;

    public CantileverBlock(Properties properties, EInsulatorType insulatorType) {
        super(properties
            .noCollission()
        );
        this.insulatorType = insulatorType;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.CANTILEVER_ITEMS.get(insulatorType));
    }

    public EInsulatorType getInsulatorType() {
        return insulatorType;
    }

    @Override
    public Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        if (level.getBlockEntity(pos) instanceof CantileverBlockEntity be) {
            int idx = 0;
            if (customData.hasPoint(index)) {
                idx = customData.getCustomDataForPoint(index).getInt(CatenaryWireItem.NBT_CANTILEVER_INDEX);
            }

            CantileverData data = be.getCantileverData()[idx];
            float size = data.width();
            float height = data.catenaryHeight();
            float xOffset = data.z();
            return switch (be.getRegistrationArmType()) {
                case OUTER -> new Vec3(xOffset - 0.5f, -height, 0.25f - size);
                case INNER -> new Vec3(xOffset - 0.5f, -height, 0.75f - size);
                default ->    new Vec3(xOffset - 0.5f, -height, 0.5f - size);
            };
        }
        return Vec3.ZERO;
    }

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData customData, int index) {
        if (level.getBlockEntity(pos) instanceof CantileverBlockEntity be) {
            int idx = 0;
            if (customData.hasPoint(index)) {
                idx = customData.getCustomDataForPoint(index).getInt(CatenaryWireItem.NBT_CANTILEVER_INDEX);
            }

            CantileverData data = be.getCantileverData()[idx];
            float size = data.width();
            float height = DragonLib.BLOCK_PIXEL * 11f + data.frontYOffset();
            float xOffset = data.z();
            return switch (be.getRegistrationArmType()) {
                case OUTER -> new Vec3(xOffset - 0.5f, height, 0.5f - size);
                case INNER -> new Vec3(xOffset - 0.5f, height, 0.5f - size);
                default ->    new Vec3(xOffset - 0.5f, height, 0.5f - size);
            };
        }
        return Vec3.ZERO;
        
    }

    @Override
    public boolean onAttachWireTo(Level level, BlockPos pos, BlockState state, Player player, Optional<UseOnContext> hit, CompoundTag pointData, int index) {
        /*
        WireGraph network = WireGraphManager.get(level, PantographsAndWires.WIRE_NET);
        if (!(level.getBlockEntity(pos) instanceof CantileverBlockEntity be)) {
            return false;
        }

        int[] connectionsCount = new int[be.getCantileversCount()];
        Arrays.fill(connectionsCount, 0);
        for (WireConnection connection : network.getConnectionsByBlock(pos)) {
            WireConnectionSyncData sync = connection.getWireConnectionSyncData();
            ListTag list = sync.getCustomData().getList(CatenaryWireItem.NBT_POINTS, Tag.TAG_COMPOUND);
            list.forEach(x -> {
                CompoundTag tag = (CompoundTag)x;
                BlockPos p = Utils.getNbtBlockPos(tag, CatenaryWireItem.NBT_POS);
                if (p.equals(pos)) {
                    int idx = tag.getInt(CatenaryWireItem.NBT_CANTILEVER_INDEX);
                    connectionsCount[idx] = connectionsCount[idx] + 1;
                }
            });
        }
        int cantileverIndex = pointData.getInt(CatenaryWireItem.NBT_CANTILEVER_INDEX);
        boolean b = connectionsCount[cantileverIndex] < 2;
        if (!b) {
            player.displayClientMessage(TextUtils.translate("block." + PantographsAndWires.MOD_ID + ".cantilever.too_many_connections").withStyle(ChatFormatting.RED), true);
        }
        return b;
        */
        return true;
    }
    
	@SuppressWarnings("deprecation")
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
		return !useContext.isSecondaryUseActive() && useContext.getItemInHand().getItem() instanceof CantileverBlockItem ? true : super.canBeReplaced(state, useContext);
	}

	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos());
        return blockstate.is(this) ? blockstate : super.getStateForPlacement(context);
	}
}
