package de.mrjulsen.paw.block;

import java.util.function.Supplier;

import de.mrjulsen.paw.block.abstractions.AbstractRotatableBlock;
import de.mrjulsen.paw.block.abstractions.IHorizontalExtensionConnectable;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import de.mrjulsen.paw.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class CantileverBracketVerticalBlock extends AbstractRotatableBlock implements IHorizontalExtensionConnectable, IWeatheringBlock<CantileverBracketVerticalBlock> {

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public static final DirectionProperty DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    
    private final IWeatheringBlock.WeatherData<CantileverBracketVerticalBlock> weatheringData;

    public CantileverBracketVerticalBlock(Properties properties, IWeatheringBlock.WeatherData<CantileverBracketVerticalBlock> weatheringData) {
        super(properties.mapColor(MapColor.METAL));

        this.weatheringData = weatheringData;

        this.registerDefaultState(defaultBlockState()
            .setValue(DIRECTION, Direction.DOWN)
        );

    }    

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.CANTILEVER_BRACKET.get(new ModBlocks.OxidizingKey(getWeatheringData().ageState(), getWeatheringData().isWaxed())).get());
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(DIRECTION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPlaceContextExtension ctxExt = (BlockPlaceContextExtension)(Object)context;
        BlockState state = super.getStateForPlacement(context);
        BlockState clickedOnState = ctxExt.paw$getPlacedOnState();
        Direction clickedFace = context.getClickedFace();
        
        if (clickedOnState.is(this) && clickedFace.getAxis().isVertical()) {
            state = state
                .setValue(DIRECTION, clickedFace)
            ;
        }

        return state;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(DIRECTION) == Direction.DOWN) {
            BlockState aboveState = level.getBlockState(pos.above());
            if (!(aboveState.getBlock() instanceof CantileverBracketVerticalBlock) && (!(aboveState.getBlock() instanceof CantileverBracketBlock) || aboveState.getValue(CantileverBracketBlock.MULTIPART_SEGMENT) != CantileverBracketBlock.DEFAULT_SEGMENT)) {
                return false;
            }
        } else if (state.getValue(DIRECTION) == Direction.UP) {
            BlockState belowState = level.getBlockState(pos.below());
            if (!(belowState.getBlock() instanceof CantileverBracketVerticalBlock) && (!(belowState.getBlock() instanceof CantileverBracketBlock) || belowState.getValue(CantileverBracketBlock.MULTIPART_SEGMENT) != CantileverBracketBlock.DEFAULT_SEGMENT)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction.getAxis() == Axis.Y) {
            if (!canSurvive(state, level, currentPos)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return state;
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Axis transformOnAxis(BlockState state) {
        return null;
    }

    @Override
    public EPostType postConnectionType(LevelReader level, BlockState state, BlockPos pos, BlockState extensionState, BlockPos extensionPos) {
        return EPostType.FENCE;
    }

    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.onRandomTick(state, level, pos, random);
    }

    public boolean isRandomlyTicking(BlockState state) {
        return getNext().isPresent();
    }

    @Override
    public @NotNull IWeatheringBlock.WeatherData<CantileverBracketVerticalBlock> getWeatheringData() {
        return weatheringData;
    }

    @Override
    public float getChanceModifier() {
        if (getWeatheringData().isWaxed()) return 0;
        return IWeatheringBlock.super.getChanceModifier();
    }
}
