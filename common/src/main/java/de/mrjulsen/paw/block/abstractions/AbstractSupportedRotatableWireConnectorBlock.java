package de.mrjulsen.paw.block.abstractions;

import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import de.mrjulsen.paw.blockentity.MultiblockWireConnectorBlockEntity;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

public abstract class AbstractSupportedRotatableWireConnectorBlock<T extends WireConnectorBlockEntity> extends AbstractRotatableWireConnectorBlock<MultiblockWireConnectorBlockEntity> {

    public AbstractSupportedRotatableWireConnectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPlaceContextExtension ctxExt = (BlockPlaceContextExtension)(Object)context;
        Direction direction = context.getClickedFace();
        BlockPos clickPos = context.getClickedPos().relative(direction.getOpposite());
        Level level = context.getLevel();
        BlockState clickedState = level.getBlockState(clickPos);

        BlockState state = super.defaultBlockState();
        Direction targetDir = direction;
        int targetRotationIdx = 1;

        if (direction.getAxis() != Axis.Y && clickedState.getBlock() instanceof AbstractRotatableBlock) { // placed at other rotated block
            targetDir = direction;
            targetRotationIdx = clickedState.getValue(ROTATION);
        } else if (direction.getAxis() != Axis.Y && canBePlacedAt(level, clickPos, clickedState, ctxExt.getPlacedOnPos(),ctxExt.getPlacedOnState(), context.getClickedFace())) { // placed at any unrotated block
            targetDir = context.getClickedFace();
            targetRotationIdx = 1;
        } else { // Placed at nothing
            int rot = (Mth.floor((double)((180.0F + context.getRotation()) * (float)TOTAL_ROTATION_STEPS / 360.0F) + 0.5) & (TOTAL_ROTATION_STEPS - 1));
            final int steps = TOTAL_ROTATION_STEPS / 4;
            Direction dir = Direction.from2DDataValue((rot + (steps / 2)) / 4);

            targetDir = dir;
            targetRotationIdx = steps - 1 - ((rot + (steps / 2)) % 4);
        }

        state = state
            .setValue(FACING, targetDir)
            .setValue(ROTATION, targetRotationIdx)
        ;
        return state;        
    }

    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return this.canSurvive(state, level, currentPos) ? super.updateShape(state, direction, neighborState, level, currentPos, neighborPos) : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos relativePos = getSupportingBlockPos(state, level, pos);
        BlockState supportState = level.getBlockState(relativePos);
        return
            (canBePlacedAt(level, pos, state, relativePos, supportState, direction.getOpposite())) &&
            (supportState.getBlock() instanceof AbstractRotatableBlock ? (int)getRelativeYRotation(supportState) : 0) == (int)getRelativeYRotation(state)
        ;
    }

    @Override
    public Vec2 getOffset(BlockState state) {
        if (state.getValue(ROTATION) < PROPERTY_MAX_ROTATION_INDEX) {
            return new Vec2(0, 0);
        }
        return switch (state.getValue(FACING)) {
            case WEST  -> new Vec2(0, -1);
            case EAST  -> new Vec2(0, 1);
            case SOUTH -> new Vec2(-1, 0);
            default    -> new Vec2(1, 0);
        };
    }

    /**
     * Gives back the {@code BlockPos} of the block that acts as support for this block. By default, the block behind this block in facing direction.
     * @param selfState The state of the block that needs to be supported
     * @param level The current level
     * @param selfPos The position of the block that needs to be supported
     * @return The position of the supporting block.
     */
    public BlockPos getSupportingBlockPos(BlockState selfState, LevelReader level, BlockPos selfPos) {
        return getSupportBlockPos(level, selfPos, selfState);
    }

    /**
     * Whether this block can be placed at any block that has a sturdy face.
     * @return The value
     */
    protected boolean allowSturdyFaceConnections() {
        return true;
    }

    /**
     * Whether this block can be placed at the selected block or not.
     * @param level The current level
     * @param pos The position of this block
     * @param state The state of this block
     * @param otherPos The position of the other selected block
     * @param otherState The state of the other selected block
     * @param direction The face this block should be placed at
     * @return Whether this block can be placed there or not.
     */
    protected boolean canBePlacedAt(LevelReader level, BlockPos pos, BlockState state, BlockPos otherPos, BlockState otherState, Direction direction) {
        return otherState.is(getSupportBlockTag()) || (allowSturdyFaceConnections() && otherState.isFaceSturdy(level, otherPos, direction));
    }

    /**
     * The {@code BlockTag} containing all blocks that are valid supporters.
     * @return The block tag.
     */
    protected abstract TagKey<Block> getSupportBlockTag();

}
