package de.mrjulsen.paw.block.abstractions;

import javax.annotation.Nullable;

import de.mrjulsen.paw.data.BlockModificationData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface IRotatableBlock {

    float getRelativeYRotation(BlockState state);

    float getYRotation(BlockState state);

    default @Nullable Axis transformOnAxis(BlockState state) {
        return null;
    }

    default Vec2 getRotationPivotPoint(BlockState state) {
        return Vec2.ZERO;
    }

    default Vec2 rotatedPivotPoint(BlockState state) {
        return getRotationPivotPoint(state);
    }

    VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context);

    @Environment(EnvType.CLIENT)
    BlockHitResult checkClickedFace(Level level, Player player, BlockHitResult original);

    default Vec2 getOffset(BlockState state) {
        return Vec2.ZERO;
    }

    default double getScaleForRotation(BlockState state) {
        return 1d / Math.cos(Math.abs(Math.toRadians(getRelativeYRotation(state))));
    }

    default @Nullable BlockModificationData onPlaceOnRotatedBlock(BlockPlaceContext context, BlockState clickedState, BlockPos clickedBlockPos) {
        if (getRelativeYRotation(clickedState) > 40 && getRelativeYRotation(clickedState) < 50) {
            return new BlockModificationData(context.getClickedPos().relative(context.getClickedFace().getCounterClockWise()), context.getClickedFace());
        }
        return null;
    }

    default @Nullable BlockModificationData onPlaceOnOtherRotatedBlock(@Nullable BlockModificationData currentModification, BlockPlaceContext context, BlockState clickedState, BlockPos clickedBlockPos) {
        return currentModification;
    }
}