package de.mrjulsen.paw.block.abstractions;

import java.util.Objects;

import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import de.mrjulsen.paw.data.BlockModificationData;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.MapCache;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec2;

public abstract class AbstractRotatedConnectableBlock extends AbstractRotatableBlock {

    public static final class BasicRotatedConnectableBlock extends AbstractRotatedConnectableBlock {
        public BasicRotatedConnectableBlock(Properties properties) {
            super(properties);
        }
    }

    public static final int DEFAULT_SEGMENT = 1;
    public static final IntegerProperty MULTIPART_SEGMENT = createMultipartSegmentsProperty();

    private final MapCache<Vec2, BlockState, BlockState> offsetCache = createOffsetCache();

    public AbstractRotatedConnectableBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL));
        this.registerDefaultState(defaultBlockState().setValue(MULTIPART_SEGMENT, DEFAULT_SEGMENT));
    }

    public static MapCache<Vec2, BlockState, BlockState> createOffsetCache() {
        return new MapCache<>((c) -> {
            int rawRotationIndex = signedRotationIndex(c);
            int rotationIndex    = Math.abs(rawRotationIndex) + DEFAULT_SEGMENT;
            int currentPart      = c.getValue(MULTIPART_SEGMENT);
            float multiplier     = (1f / (float) rotationIndex) * (MathUtils.clamp(currentPart, DEFAULT_SEGMENT, rotationIndex) - DEFAULT_SEGMENT);
            return switch (c.getValue(FACING)) {
                case WEST  -> new Vec2(0,  1).scale(multiplier);
                case EAST  -> new Vec2(0, -1).scale(multiplier);
                case SOUTH -> new Vec2(1,  0).scale(multiplier);
                default    -> new Vec2(-1, 0).scale(multiplier);
            };
        }, (state) -> Objects.hash(state.getValues().values().toArray(Object[]::new)), ECachingPriority.ALWAYS);
    }

    public static IntegerProperty createMultipartSegmentsProperty() {
        return IntegerProperty.create("multipart_segment", DEFAULT_SEGMENT, AbstractRotatableBlock.ROTATIONS);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(MULTIPART_SEGMENT);
    }

    protected int maxSegments(BlockState state) {
        int rotationIndex = Math.abs(signedRotationIndex(state)) + 1;
        if (rotationIndex >= ROTATIONS + 1) {
            rotationIndex = 1;
        }
        return rotationIndex;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPlaceContextExtension ctxExt      = (BlockPlaceContextExtension)(Object) context;
        BlockState                 state        = super.getStateForPlacement(context);
        BlockState                 clickedOnState = ctxExt.getPlacedOnState();
        Direction                  clickedFace  = context.getClickedFace();

        if (canConnect(context.getLevel(), state, context.getClickedPos(), clickedOnState, ctxExt.getPlacedOnPos())
                && clickedOnState.getValue(FACING).getAxis() == clickedFace.getAxis()) {
            int rotationIndex = maxSegments(clickedOnState);
            state = state
                    .setValue(MULTIPART_SEGMENT,
                            clickedFace == clickedOnState.getValue(FACING)
                                    ? (clickedOnState.getValue(MULTIPART_SEGMENT) % rotationIndex) + 1
                                    : Math.abs((clickedOnState.getValue(MULTIPART_SEGMENT) - 2) % rotationIndex) + 1)
                    .setValue(FACING,   clickedOnState.getValue(FACING))
                    .setValue(ROTATION, clickedOnState.getValue(ROTATION));
        }

        return state;
    }

    protected boolean canConnect(Level level, BlockState state, BlockPos pos, BlockState otherState, BlockPos otherPos) {
        return state.is(otherState.getBlock());
    }

    @Override
    public Vec2 getOffset(BlockState state) {
        return offsetCache.get(state, state);
    }

    @Override
    public Axis transformOnAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public BlockModificationData onPlaceOnRotatedBlock(BlockPlaceContext context, BlockState clickedState, BlockPos clickedBlockPos) {
        BlockModificationData value        = super.onPlaceOnRotatedBlock(context, clickedState, clickedBlockPos);
        int                   rotationValue = signedRotationIndex(clickedState);
        Direction             clickedFace  = context.getClickedFace();
        boolean               clickedOnFront = clickedFace == clickedState.getValue(FACING);

        if (value == null && clickedFace != null && rotationValue != 0) {
            boolean segmentCondition = clickedState.getValue(MULTIPART_SEGMENT)
                    == AbstractRotatableBlock.ROTATIONS / 2 + (clickedOnFront ^ rotationValue < 0 ? 1 : 0);
            if (segmentCondition && clickedFace.getAxis() == transformOnAxis(clickedState)) {
                return new BlockModificationData(
                        context.getClickedPos().relative(rotationValue > 0 ? clickedFace.getCounterClockWise() : clickedFace.getClockWise()),
                        clickedFace
                );
            }
        }
        return value;
    }

    @Override
    public BlockModificationData onPlaceOnOtherRotatedBlock(BlockModificationData currentModification, BlockPlaceContext context, BlockState clickedState, BlockPos clickedBlockPos) {
        int       rotationValue = signedRotationIndex(clickedState);
        Direction clickedFace   = context.getClickedFace();

        if (clickedFace != null && rotationValue < 0 && !(clickedState.getBlock() instanceof AbstractRotatedConnectableBlock)) {
            return new BlockModificationData(context.getClickedPos().relative(clickedFace.getClockWise()), clickedFace);
        }
        return currentModification;
    }

    protected BlockPos relativeTo(BlockAndTintGetter level, BlockState state, BlockPos pos, Direction direction) {
        BlockPos result = pos.relative(direction);
        if (!level.getBlockState(pos).is(state.getBlock())) {
            return result;
        }

        int rot = signedRotationIndex(state);
        if (rot >= ROTATIONS) {
            result = result.relative(direction.getCounterClockWise());
        } else if (rot > 0 && state.getValue(MULTIPART_SEGMENT) == AbstractRotatableBlock.ROTATIONS / 2) {
            result = result.relative(direction.getCounterClockWise());
        } else if (rot < 0 && state.getValue(MULTIPART_SEGMENT) == AbstractRotatableBlock.ROTATIONS / 2 + 1) {
            result = result.relative(direction.getClockWise());
        }
        return result;
    }
}