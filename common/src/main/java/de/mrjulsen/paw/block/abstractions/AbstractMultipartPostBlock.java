package de.mrjulsen.paw.block.abstractions;

import de.mrjulsen.paw.block.property.EPostPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec2;

public abstract class AbstractMultipartPostBlock extends AbstractSimplePostBlock {

    public static final EnumProperty<EPostPart> PART = EnumProperty.create("part", EPostPart.class);

    public AbstractMultipartPostBlock(Properties properties) {
        super(Properties.of().mapColor(MapColor.METAL));

        this.registerDefaultState(defaultBlockState()
            .setValue(PART, EPostPart.IN_BETWEEN)
        );
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {        
        BlockState state = super.getStateForPlacement(context);
        Level level = context.getLevel();
        BlockPos clickPos = context.getClickedPos();
        BlockState aboveState = level.getBlockState(clickPos.above());
        BlockState belowState = level.getBlockState(clickPos.below());

        if (aboveState.is(this) && belowState.is(this)) {
            state = state
                .setValue(PART, EPostPart.IN_BETWEEN)
            ;
        } else if (belowState.is(this)) {
            state = state
                .setValue(PART, EPostPart.TOP)
            ;
        } else {
            state = state
                .setValue(PART, EPostPart.BOTTOM)
            ;
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction.getAxis() == Axis.Y) {            
            BlockState aboveState = level.getBlockState(currentPos.above());
            BlockState belowState = level.getBlockState(currentPos.below());

            if (aboveState.is(this) && belowState.is(this)) {
                state = state
                    .setValue(PART, EPostPart.IN_BETWEEN)
                ;
            } else if (belowState.is(this)) {
                state = state
                    .setValue(PART, EPostPart.TOP)
                ;
            } else {
                state = state
                    .setValue(PART, EPostPart.BOTTOM)
                ;
            }
        }
        return state;
    }

    @Override
    public Vec2 getRotationPivotPoint(BlockState state) {
        return Vec2.ZERO;
    }
}
