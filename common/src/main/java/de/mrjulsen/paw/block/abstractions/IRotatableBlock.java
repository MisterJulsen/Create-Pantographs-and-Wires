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

/**
 * The basis for blocks that can be rotated more than 90 degrees.
 */
public interface IRotatableBlock {

    /**
     * The y rotation of the block relative to the current facing direction.
     * @param state The blockstate.
     * @return The rotation value in degrees.
     */
    float getRelativeYRotation(BlockState state);
    
    /**
     * The y rotation of the block.
     * @param state The blockstate.
     * @return The rotation value in degrees.
     */
    float getYRotation(BlockState state);

    /**
     * The axis on which the model should be scaled when rotating so that blocks can connect. Pass {@code null} to disable scaling. Only horizontal axes allowed!
     * @param state The blockstate.
     * @return The scaling axis.
     */
    default @Nullable Axis transformOnAxis(BlockState state) {
        return null;
    }

    /**
     * The pivot point, relative to the block center, at which the block is rotated.
     * @param state The blockstate.
     * @return The pivot point vector.
     */
    default Vec2 getRotationPivotPoint(BlockState state) {
        return Vec2.ZERO;
    }

    /**
     * By default, the pivot point is constant, since the block itself is constant and
     * only the model and the hitbox rotate. Blocks that influence the rotation
     * themselves (e.g. through a {@code FACING} direction), the pivot point can be subsequently
     * rotated accordingly. The basis is the original pivot point (from the block center)
     * and not from the block origin.
     * @param state
     * @return The rotated pivot point.
     */
    default Vec2 rotatedPivotPoint(BlockState state) {
        return getRotationPivotPoint(state);
    }

    /**
     * The default shape of the block, ideally consisting of a single {@code VoxelShape}. This shape is used for rotation.
     * @param state The blockstate.
     * @param level The level the block is placed in.
     * @param pos The position of the block.
     * @param context The collision context.
     * @return The {@code VoxelShape}.
     */
    VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context);

    /**
     * The side that was clicked. Since the rotated hitbox is only approximated by small voxel shapes,
     * placing blocks on that block would be unpredictable depending on which side of the small voxel shapes
     * was clicked. This method fixes this problem and manipulates the {@code BlockHitResult} so that the
     * placement behavior makes sense to the player.
     * @param level The level the action occurs.
     * @param player The player interacting with this block.
     * @param original The vanilla {@code BlockHitResult} which should be updated.
     * @return The updated {@code BlockHitResult}.
     */
    @Environment(EnvType.CLIENT)
    BlockHitResult checkClickedFace(Level level, Player player, BlockHitResult original);

    /**
     * The offset by which the block is moved from its original position. This value is not affected by any rotation transformation.
     * @param state The blockstate.
     * @return The offset vector.
     */
    default Vec2 getOffset(BlockState state) {
        return Vec2.ZERO;
    }

    /**
     * The scale value used to scale the block when rotating. Corresponds to the hypotenuse of the triangle created by the rotation.
     * @param state The blockstate.
     * @return The scale multiplier.
     */
    default double getScaleForRotation(BlockState state) {
        return 1d / Math.cos(Math.abs(Math.toRadians(getRelativeYRotation(state))));
    }

    /**
     * Modifies the placement of other blocks when placed on a side of this block. Return the edited values
     * ​​as {@code Pair<>}(new block pos, new direction) or {@code null} if the placement should not be modified.
     * @param context The current {@code BlockPlaceContext}.
     * @param clickedState The {@code BlockState} that this block was placed on.
     * @param clickedBlockPos The {@code BlockPos} of the block that this block was placed on.
     * @return A new {@code BlockModificationData} containing the modified placement data or {@code null} for default placement.
     */
    default @Nullable BlockModificationData onPlaceOnRotatedBlock(BlockPlaceContext context, BlockState clickedState, BlockPos clickedBlockPos) {
        if (getRelativeYRotation(clickedState) > 40 && getRelativeYRotation(clickedState) < 50) {
            return new BlockModificationData(context.getClickedPos().relative(context.getClickedFace().getCounterClockWise()), context.getClickedFace());
        }
        return null;
    }

    /**
     * Called when this block is placed on another {@code IRotatableBlock} and after the placement modification has been calculated.
     * @param currentModification The current calculated placement modification data. Can be {@code null} if the support block hasn't changed the placement.
     * @param context The default placement context.
     * @param clickedState The {@code BlockState} that this block was placed on.
     * @param clickedBlockPos The {@code BlockPos} of the block that this block was placed on.
     * @return A new {@code BlockModificationData}, the content of the current {@code BlockModificationData} or {@code null} for default placement.
     */
    default @Nullable BlockModificationData onPlaceOnOtherRotatedBlock(@Nullable BlockModificationData currentModification, BlockPlaceContext context, BlockState clickedState, BlockPos clickedBlockPos) {
        return currentModification;
    }
}
