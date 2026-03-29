package de.mrjulsen.paw.block.abstractions;

import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
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

public abstract class AbstractSupportedRotatableWireConnectorBlock<T extends WireConnectorBlockEntity> extends AbstractRotatableWireConnectorBlock<T> {

    public AbstractSupportedRotatableWireConnectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPlaceContextExtension ctxExt      = (BlockPlaceContextExtension)(Object) context;
        Direction                  direction   = context.getClickedFace();
        BlockPos                   clickPos    = context.getClickedPos().relative(direction.getOpposite());
        Level                      level       = context.getLevel();
        BlockState                 clickedState = level.getBlockState(clickPos);

        Direction targetDir          = direction;
        int       targetRotationIdx  = PROPERTY_BASE_ROTATION_INDEX;

        if (direction.getAxis() != Axis.Y && clickedState.getBlock() instanceof AbstractRotatableBlock) {
            targetDir         = direction;
            targetRotationIdx = clickedState.getValue(ROTATION);
        } else if (direction.getAxis() != Axis.Y
                && canBePlacedAt(level, clickPos, clickedState, ctxExt.getPlacedOnPos(), ctxExt.getPlacedOnState(), context.getClickedFace())) {
            targetDir         = context.getClickedFace();
            targetRotationIdx = PROPERTY_BASE_ROTATION_INDEX;
        } else {
            int rot = Mth.floor(
                    (180.0f + context.getRotation()) * (float) TOTAL_ROTATION_STEPS / 360.0f + 0.5f)
                    & (TOTAL_ROTATION_STEPS - 1);
            final int stepsPerSide = TOTAL_ROTATION_STEPS / 4;
            targetDir         = Direction.from2DDataValue((rot + stepsPerSide / 2) / stepsPerSide);
            targetRotationIdx = stepsPerSide - 1 - ((rot + stepsPerSide / 2) % stepsPerSide);
        }

        return defaultBlockState()
                .setValue(FACING,   targetDir)
                .setValue(ROTATION, targetRotationIdx);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return canSurvive(state, level, currentPos)
                ? super.updateShape(state, direction, neighborState, level, currentPos, neighborPos)
                : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction  direction     = state.getValue(FACING);
        BlockPos   relativePos   = getSupportingBlockPos(state, level, pos);
        BlockState supportState  = level.getBlockState(relativePos);

        int supportRotation = supportState.getBlock() instanceof AbstractRotatableBlock
                ? (int) getRelativeYRotation(supportState)
                : 0;

        return canBePlacedAt(level, pos, state, relativePos, supportState, direction.getOpposite())
                && supportRotation == (int) getRelativeYRotation(state);
    }

    @Override
    public Vec2 getOffset(BlockState state) {
        if (state.getValue(ROTATION) < PROPERTY_MAX_ROTATION_INDEX) {
            return Vec2.ZERO;
        }
        return switch (state.getValue(FACING)) {
            case WEST  -> new Vec2(0, -1);
            case EAST  -> new Vec2(0,  1);
            case SOUTH -> new Vec2(-1, 0);
            default    -> new Vec2(1,  0);
        };
    }

    public BlockPos getSupportingBlockPos(BlockState selfState, LevelReader level, BlockPos selfPos) {
        return getSupportBlockPos(level, selfPos, selfState);
    }

    protected boolean allowSturdyFaceConnections() {
        return true;
    }

    protected boolean canBePlacedAt(LevelReader level, BlockPos pos, BlockState state, BlockPos otherPos, BlockState otherState, Direction direction) {
        return otherState.is(getSupportBlockTag())
                || (allowSturdyFaceConnections() && otherState.isFaceSturdy(level, otherPos, direction));
    }

    protected abstract TagKey<Block> getSupportBlockTag();
}