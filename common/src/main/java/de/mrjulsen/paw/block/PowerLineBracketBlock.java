package de.mrjulsen.paw.block;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import de.mrjulsen.paw.block.abstractions.AbstractRotatedConnectableBlock;
import de.mrjulsen.paw.block.abstractions.IHorizontalExtensionConnectable;
import de.mrjulsen.paw.block.abstractions.IRotatableBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.abstractions.IHorizontalExtensionConnectable.EPostType;
import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import de.mrjulsen.paw.data.BlockModificationData;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.MapCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class PowerLineBracketBlock extends AbstractRotatedConnectableBlock implements IWeatheringBlock<PowerLineBracketBlock> {

    public static enum EConnectionType implements StringRepresentable {
        NONE("none"),
        ON_POST("on_post"),
        ON_POST_EXTENSION("on_post_extension"),
        AT_POST("at_post");

        String name;

        EConnectionType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private static record TransformationShapeKey(ShapeKey shapeKey, Direction direction, int rotation, Half half, BlockState state) {
        @Override
        public final boolean equals(Object other) {
            if (other instanceof TransformationShapeKey o) {
                return shapeKey().equals(o.shapeKey()) && direction().equals(o.direction()) && rotation() == o.rotation() && half() == o.half();
            }
            return false;
        }
        @Override
        public final int hashCode() {
            return Objects.hash(shapeKey(), direction(), rotation(), half());
        }
    }
    private static record ShapeKey(EPostType postType, EConnectionType connectionType) {
        @Override
        public final int hashCode() {
            return Objects.hash(postType(), connectionType());
        }
    }

    private static final VoxelShape DEFAULT_SHAPE = Block.box(6.5, 0, 0, 9.5, 3, 16);
    private static final Map<ShapeKey, VoxelShape> BASE_SHAPES = Map.ofEntries(
        Map.entry(new ShapeKey(EPostType.FENCE, EConnectionType.ON_POST), Block.box(5, 0, 5, 11, 3, 11)),
        Map.entry(new ShapeKey(EPostType.FENCE, EConnectionType.ON_POST_EXTENSION), Shapes.or(Block.box(5, 0, 16, 11, 3, 21), Block.box(6.5, 0, 0, 9.5, 3, 16))),
        Map.entry(new ShapeKey(EPostType.FENCE, EConnectionType.AT_POST), Shapes.or(Block.box(5, 0, 16, 11, 3, 21), Block.box(6.5, 0, 0, 9.5, 3, 16))),
        
        Map.entry(new ShapeKey(EPostType.WALL, EConnectionType.ON_POST), Block.box(4, 0, 4, 12, 3, 12)),
        Map.entry(new ShapeKey(EPostType.WALL, EConnectionType.ON_POST_EXTENSION), Shapes.or(Block.box(4, 0, 12, 12, 3, 20), Block.box(6.5, 0, 0, 9.5, 3, 12))),
        Map.entry(new ShapeKey(EPostType.WALL, EConnectionType.AT_POST), Shapes.or(Block.box(4, 0, 12, 12, 3, 20), Block.box(6.5, 0, 0, 9.5, 3, 12))),

        Map.entry(new ShapeKey(EPostType.LATTICE, EConnectionType.ON_POST), Block.box(2, 0, 2, 14, 3, 14)),
        Map.entry(new ShapeKey(EPostType.LATTICE, EConnectionType.ON_POST_EXTENSION), Shapes.or(Block.box(2, 0, 5, 14, 3, 18), Block.box(6.5, 0, 0, 9.5, 3, 5))),
        Map.entry(new ShapeKey(EPostType.LATTICE, EConnectionType.AT_POST), Shapes.or(Block.box(2, 0, 5, 14, 3, 18), Block.box(6.5, 0, 0, 9.5, 3, 5)))
    );
    private static final MapCache<VoxelShape, TransformationShapeKey, TransformationShapeKey> shapesCache = new MapCache<>((key) -> {
        VoxelShape baseShape = key.shapeKey().postType() == EPostType.NONE || key.shapeKey().connectionType() == EConnectionType.NONE ? DEFAULT_SHAPE : BASE_SHAPES.get(key.shapeKey());        
        Direction direction = key.direction();
        VoxelShape shape = ModMath.rotateShape(baseShape, Axis.Y, (int)direction.getOpposite().toYRot());
        shape = ModMath.scaleShape(shape, direction.getAxis(), key.state().getBlock() instanceof IRotatableBlock rot ? rot.getScaleForRotation(key.state()) : 1, 0.5f);
        if (key.half() == Half.TOP) {
            shape = ModMath.moveShape(shape, new Vec3(0, 1f / 16f * 13, 0));
        }
        return shape;
    }, TransformationShapeKey::hashCode, ECachingPriority.ALWAYS);

    public static final EnumProperty<EConnectionType> CONNECTION_TYPE = EnumProperty.create("connection_type", EConnectionType.class);
    public static final EnumProperty<EPostType> POST_TYPE = EnumProperty.create("post_type", EPostType.class);
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    
    private final WeatheringData<PowerLineBracketBlock> weatheringData;

    
    public PowerLineBracketBlock(Properties properties, WeatheringData<PowerLineBracketBlock> weatheringData) {
        super(properties
            .noOcclusion()
        );

        this.weatheringData = weatheringData;

        this.registerDefaultState(this.defaultBlockState()
            .setValue(POST_TYPE, EPostType.NONE)
            .setValue(CONNECTION_TYPE, EConnectionType.NONE)
            .setValue(HALF, Half.BOTTOM)
        );
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(CONNECTION_TYPE, POST_TYPE, HALF);
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {        
        TransformationShapeKey key = new TransformationShapeKey(new ShapeKey(state.getValue(POST_TYPE), state.getValue(CONNECTION_TYPE)), state.getValue(FACING), normalizedPropertyRotationIndex(state), state.getValue(HALF), state);
        return shapesCache.get(key, key);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPlaceContextExtension contextExt = (BlockPlaceContextExtension)(Object)context;
        Level level = context.getLevel();
        BlockState state = super.getStateForPlacement(context);
        BlockPos pos = context.getClickedPos();
        BlockState supportState = contextExt.getPlacedOnState();
        BlockPos supportPos = contextExt.getPlacedOnPos();
        Direction clickedFace = context.getClickedFace();

        if (supportState.getBlock() instanceof PowerLineBracketBlock && supportState.getValue(FACING).getAxis() == clickedFace.getAxis()) {
            if (supportState.getValue(CONNECTION_TYPE) == EConnectionType.ON_POST) {
                state = state
                    .setValue(CONNECTION_TYPE, EConnectionType.ON_POST_EXTENSION)
                ;
            }
            state = state
                .setValue(FACING, clickedFace)
                .setValue(POST_TYPE, supportState.getValue(POST_TYPE))
                .setValue(HALF, supportState.getValue(HALF))
            ;
            
        } else if (supportState.getBlock() instanceof IHorizontalExtensionConnectable connection) {
            state = state
                .setValue(POST_TYPE, connection.postConnectionType(level, supportState, supportPos, state, pos))
                .setValue(ROTATION, supportState.getValue(ROTATION))
                .setValue(HALF, context.getClickLocation().y - (double)pos.getY() > 0.5f ? Half.TOP : Half.BOTTOM)
            ;
            if (clickedFace == Direction.UP) {
                state = state
                    .setValue(CONNECTION_TYPE, EConnectionType.ON_POST)
                    .setValue(FACING, supportState.getValue(FACING))
                ;
            } else if (clickedFace.getAxis().isHorizontal()) {
                int rotationValue = Math.abs(normalizedPropertyRotationIndex(supportState));
                state = state
                    .setValue(CONNECTION_TYPE, EConnectionType.AT_POST)
                    .setValue(FACING, clickedFace)
                    .setValue(MULTIPART_SEGMENT, rotationValue > 0 && rotationValue < ROTATIONS ? 2 : 1)
                ;
            }
        }

        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return canSurvive(state, level, currentPos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos supportPos = switch (state.getValue(CONNECTION_TYPE)) {
            case ON_POST -> pos.below();
            default -> relativeTo(level, state, pos, state.getValue(FACING).getOpposite());
        };
        BlockState supportState = level.getBlockState(supportPos);
        return (supportState.getBlock() instanceof PowerLineBracketBlock && supportState.getValue(FACING).getAxis() == state.getValue(FACING).getAxis()) || supportState.getBlock() instanceof IHorizontalExtensionConnectable;
    }

    @Override
    public BlockModificationData onPlaceOnOtherRotatedBlock(BlockModificationData currentModification, BlockPlaceContext context, BlockState clickedState, BlockPos clickedBlockPos) {
        Direction clickedFace = context.getClickedFace();
        int rot = normalizedPropertyRotationIndex(clickedState);
        boolean oppositeFacing = 
            clickedState.getBlock() instanceof PowerLineBracketBlock && 
            clickedState.getValue(CONNECTION_TYPE) == EConnectionType.ON_POST &&
            clickedState.getValue(FACING).getOpposite() == clickedFace &&
            rot < ROTATIONS
        ;

        if (oppositeFacing) {
            if (rot < 0)
                return new BlockModificationData(context.getClickedPos().relative(clickedFace.getClockWise()), clickedFace);
            return null;
        }
        return super.onPlaceOnOtherRotatedBlock(currentModification, context, clickedState, clickedBlockPos);
    }

    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.onRandomTick(state, level, pos, random);
    }

    public boolean isRandomlyTicking(BlockState state) {
        return getNext().isPresent();
    }

    @Override
    public @NotNull WeatheringData<PowerLineBracketBlock> getWeatheringData() {
        return weatheringData;
    }

    @Override
    public float getChanceModifier() {
        if (getWeatheringData().isWaxed()) return 0;
        return IWeatheringBlock.super.getChanceModifier();
    }
}
