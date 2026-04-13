package de.mrjulsen.paw.block;

import java.util.Optional;

import javax.annotation.Nullable;

import com.eliotlash.mclib.utils.MathUtils;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MathUtil;

public class CantileverBlock extends AbstractCantileverBlock {

    private final EInsulatorType insulatorType;

    public CantileverBlock(Properties properties, EInsulatorType insulatorType) {
        super(properties
            .noCollission()
        );
        this.insulatorType = insulatorType;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(ModBlocks.CANTILEVER_ITEMS.get(insulatorType).get());
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
            idx = MathUtils.clamp(idx, 0, Math.max(be.getSubCantileverSettings().size() - 1, 0));

            CantileverData data = be.getCantileverData()[idx];
            int count = be.getSubCantileverSettings().size();
            float size = data.width();
            float height = data.catenaryHeight();
            float xOffset = data.z();
            float z = 0.5f - size;

            if (count <= 1) {
                z -= data.settings().registrationArm().getOffset();
            }
            return new Vec3(xOffset - 0.5f, -height, z);
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
            idx = MathUtils.clamp(idx, 0, Math.max(be.getSubCantileverSettings().size() - 1, 0));

            CantileverData data = be.getCantileverData()[idx];
            float size = data.width();
            float height = DragonLib.BLOCK_PIXEL * 11f + data.frontYOffset();
            float xOffset = data.z();
            return new Vec3(xOffset - 0.5f, height, 0.5f - size);
            /*
            return switch (be.getRegistrationArmType()) {
                case OUTER -> new Vec3(xOffset - 0.5f, height, 0.5f - size);
                case INNER -> new Vec3(xOffset - 0.5f, height, 0.5f - size);
                default ->    new Vec3(xOffset - 0.5f, height, 0.5f - size);
            };

             */
        }
        return Vec3.ZERO;
        
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
