package de.mrjulsen.paw.block;

import java.util.Objects;

import de.mrjulsen.paw.util.Const;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.wires.item.CustomData;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.MapCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class InsulatorBlock extends AbstractPlaceableInsulatorBlock {

    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    private static final VoxelShape SHAPE_BOTTOM = Block.box(5, 2, 5, 11, 16, 11);
    private static final VoxelShape SHAPE_TOP = Block.box(5, 0, 5, 11, 10, 11);

    
    protected static record TransformationShapeKeyExtension(Direction direction, int rotation, Half half, BlockState state) {
        @Override
        public final boolean equals(Object other) {
            if (other instanceof TransformationShapeKeyExtension o) {
                return direction().equals(o.direction()) && rotation() == o.rotation() && half() == o.half();
            }
            return false;
        }
        @Override
        public final int hashCode() {
            return Objects.hash(direction(), rotation(), half());
        }
    }

    private final MapCache<VoxelShape, TransformationShapeKeyExtension, TransformationShapeKeyExtension> shapesCache;

    public InsulatorBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL)
            .noOcclusion()
        );
        this.shapesCache = new MapCache<>((key) -> {
            VoxelShape baseShape = key.half() == Half.TOP ? SHAPE_TOP : SHAPE_BOTTOM;
            Direction direction = key.direction();
            VoxelShape result = ModMath.rotateShape(baseShape, Axis.Y, (int)direction.getOpposite().toYRot());    
            return result;
        }, TransformationShapeKeyExtension::hashCode, ECachingPriority.ALWAYS);

        this.registerDefaultState(defaultBlockState()
            .setValue(HALF, Half.TOP)
        );
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos clickPos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState state = super.getStateForPlacement(context);

        BlockPos abovePos = clickPos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        BlockPos belowPos = clickPos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        if (aboveState.getBlock() instanceof PowerLineBracketBlock) {
            state = state
                .setValue(MULTIPART_SEGMENT, aboveState.getValue(MULTIPART_SEGMENT))
                .setValue(FACING, aboveState.getValue(FACING))
                .setValue(ROTATION, aboveState.getValue(ROTATION))
                .setValue(HALF, Half.BOTTOM)
            ;
        } else if (belowState.getBlock() instanceof PowerLineBracketBlock) {
            state = state
                .setValue(MULTIPART_SEGMENT, belowState.getValue(MULTIPART_SEGMENT))
                .setValue(FACING, belowState.getValue(FACING))
                .setValue(ROTATION, belowState.getValue(ROTATION))
                .setValue(HALF, Half.TOP)
            ;
        } else {
            state = state
                .setValue(HALF, context.getClickedFace() == Direction.UP ? Half.TOP : Half.BOTTOM)
            ;
        }
        return state;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        return switch (state.getValue(HALF)) {
            case TOP -> canSupportCenter(level, belowPos, Direction.UP) || (belowState.getBlock() instanceof PowerLineBracketBlock && belowState.getValue(PowerLineBracketBlock.HALF) == Half.TOP);
            default -> canSupportCenter(level, abovePos, Direction.DOWN) || (aboveState.getBlock() instanceof PowerLineBracketBlock && aboveState.getValue(PowerLineBracketBlock.HALF) == Half.BOTTOM);
        };
    }

    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return !this.canSurvive(state, level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        TransformationShapeKeyExtension key = new TransformationShapeKeyExtension(state.getValue(FACING), normalizedPropertyRotationIndex(state), state.getValue(HALF), state);
        return shapesCache.get(key, key);
    }

    @Override
    public Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData itemData, int index) {
        return state.getValue(HALF) == Half.TOP ? new Vec3(0, Const.PIXEL * 10f, 0) : new Vec3(0, Const.PIXEL * 3f, 0);
    }
}
