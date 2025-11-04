package de.mrjulsen.paw.block;

import java.util.Objects;
import java.util.function.Supplier;

import de.mrjulsen.paw.block.abstractions.AbstractRotatedConnectableBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.MapCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class CantileverBracketBaseBlock<T extends CantileverBracketBaseBlock<T>> extends AbstractRotatedConnectableBlock implements IWeatheringBlock<T> {
    
    protected static final record ShapeContext(BlockGetter level, BlockPos pos, BlockState state, CollisionContext context) {}

    private final MapCache<VoxelShape, BlockState, ShapeContext> shapeContext = new MapCache<>(c -> makeShape(c), (state) -> Objects.hash(state.getValues().values().toArray(Object[]::new)), ECachingPriority.ALWAYS);
    
    protected final WeatherState weatherState;
    protected final Supplier<T> nextOxidationState;

    public CantileverBracketBaseBlock(Properties properties, WeatherState weatherState, Supplier<T> nextOxidationState) {
        super(properties);
        this.weatherState = weatherState;
        this.nextOxidationState = nextOxidationState;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.CANTILEVER_BRACKET.get(weatherState).get());
    }

    protected VoxelShape makeShape(ShapeContext c) {      
        double stretch = 16d * ((1d / Math.cos(Math.abs(Math.toRadians(getRelativeYRotation(c.state()))))) - 1d);
        double halfStretch = stretch / 2;
        return switch (c.state().getValue(FACING)) {            
            case SOUTH -> Block.box(6d, 6d, 0 - halfStretch, 10d, 10d, 16d + halfStretch);
            case WEST  -> Block.box(0 - halfStretch, 6d, 6d, 16d + halfStretch, 10d, 10d);
            case EAST  -> Block.box(0 - halfStretch, 6d, 6d, 16d + halfStretch, 10d, 10d);
            default    -> Block.box(6d, 6d, 0 - halfStretch, 10d, 10d, 16d + halfStretch);
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPlaceContextExtension ctxExt = (BlockPlaceContextExtension)(Object)context;
        BlockState state = super.getStateForPlacement(context);
        BlockState clickedOnState = ctxExt.getPlacedOnState();
        Direction clickedFace = context.getClickedFace();
        
        if ((clickedOnState.getBlock() instanceof CantileverBracketBaseBlock || clickedOnState.getBlock() instanceof CantileverBracketVerticalBlock) && clickedFace.getAxis().isVertical()) {
            state = ModBlocks.CANTILEVER_BRACKET_VERTICAL.get(weatherState).getDefaultState()
                .setValue(CantileverBracketVerticalBlock.DIRECTION, clickedFace)
                .setValue(FACING, clickedOnState.getValue(FACING))
                .setValue(ROTATION, clickedOnState.getValue(ROTATION))
            ;
        }

        return state;
    }

    @Override
    protected boolean canConnect(Level level, BlockState state, BlockPos pos, BlockState otherState, BlockPos otherPos) {
        return otherState.getBlock() instanceof CantileverBracketBaseBlock;
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeContext.get(new ShapeContext(level, pos, state, context), state);
    }

    @Override
    public Axis transformOnAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.onRandomTick(state, level, pos, random);
    }

    public boolean isRandomlyTicking(BlockState state) {
        return getNext(state.getBlock()).isPresent();
    }

    @Override
    public WeatherState getAge() {
        return weatherState;
    }

    @Override
    public Supplier<T> getNextState() {
        return nextOxidationState;
    }
}
