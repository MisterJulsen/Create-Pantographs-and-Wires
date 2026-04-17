package de.mrjulsen.paw.block;

import java.util.function.Supplier;

import de.mrjulsen.paw.block.abstractions.IHorizontalExtensionConnectable;
import de.mrjulsen.paw.block.abstractions.IHorizontalExtensionConnectable.EPostType;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CantileverBracketBlock extends CantileverBracketBaseBlock<CantileverBracketBlock> {
    
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public CantileverBracketBlock(Properties properties, IWeatheringBlock.WeatherData<CantileverBracketBlock> weatheringData) {
        super(properties.mapColor(MapColor.METAL), weatheringData);

        this.registerDefaultState(defaultBlockState()
            .setValue(DOWN, false)
            .setValue(UP, false)
        );
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(ModBlocks.CANTILEVER_BRACKET.get(new ModBlocks.OxidizingKey(getWeatheringData().ageState(), getWeatheringData().isWaxed())).get());
    }

    @Override
    protected VoxelShape makeShape(ShapeContext c) {
        VoxelShape[] shapes = new VoxelShape[] { Shapes.empty(), Shapes.empty() };
        if (c.state().getValue(DOWN)) {
            shapes[0] = Block.box(6, 0, 6, 10, 8, 10);
        }
        if (c.state().getValue(UP)) {
            shapes[1] = Block.box(6, 8, 6, 10, 16, 10);
        }
        return Shapes.or(super.makeShape(c), shapes);
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(DOWN, UP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);

        BlockPos supportPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState supportState = context.getLevel().getBlockState(supportPos);
        boolean supportIsPost = supportState.getBlock() instanceof IHorizontalExtensionConnectable conn && conn.postConnectionType(context.getLevel(), supportState, supportPos, state, context.getClickedPos()) == EPostType.LATTICE && context.getClickedFace().getAxis() != Axis.Y;
        
        if (supportIsPost) {
            state = ModBlocks.CANTILEVER_BRACKET_AT_POST.get(new ModBlocks.OxidizingKey(getWeatheringData().ageState(), getWeatheringData().isWaxed())).getDefaultState()
                .setValue(FACING, context.getClickedFace())
                .setValue(ROTATION, supportState.getValue(ROTATION))
                .setValue(MULTIPART_SEGMENT, maxSegments(supportState))
            ;
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite()) {
            // TODO: Diagonal checken bei 45 Grad
            BlockPos supportPos = currentPos.relative(direction);
            BlockState supportState = level.getBlockState(supportPos);
            boolean supportIsPost = supportState.getBlock() instanceof IHorizontalExtensionConnectable conn && conn.postConnectionType(level, supportState, supportPos, state, currentPos) == EPostType.LATTICE;

            Direction facing = state.getValue(FACING);
            int rotation = state.getValue(ROTATION);
            int segment = state.getValue(MULTIPART_SEGMENT);
            
            if (supportIsPost && supportState.getValue(ROTATION) == rotation) {
                state = ModBlocks.CANTILEVER_BRACKET_AT_POST.get(new ModBlocks.OxidizingKey(getWeatheringData().ageState(), getWeatheringData().isWaxed())).getDefaultState()
                    .setValue(FACING, facing)
                    .setValue(ROTATION, rotation)
                    .setValue(MULTIPART_SEGMENT, segment)
                ;
            }
        } else if (direction.getAxis() == Axis.Y) {
            BlockState belowState = level.getBlockState(currentPos.below());
            BlockState aboveState = level.getBlockState(currentPos.above());
            boolean aboveIsVertical = aboveState.getBlock() instanceof CantileverBracketVerticalBlock;
            boolean belowIsVertical = belowState.getBlock() instanceof CantileverBracketVerticalBlock;
            
            state = state
                .setValue(UP, aboveIsVertical)
                .setValue(DOWN, belowIsVertical)
            ;
        }
        return state;
    }
}
