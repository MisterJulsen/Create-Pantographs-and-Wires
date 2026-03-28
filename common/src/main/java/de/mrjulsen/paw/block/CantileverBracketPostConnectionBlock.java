package de.mrjulsen.paw.block;

import java.util.function.Supplier;

import de.mrjulsen.paw.block.abstractions.IHorizontalExtensionConnectable;
import de.mrjulsen.paw.block.abstractions.IHorizontalExtensionConnectable.EPostType;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class CantileverBracketPostConnectionBlock extends CantileverBracketBaseBlock<CantileverBracketPostConnectionBlock> {

    public CantileverBracketPostConnectionBlock(Properties properties, WeatheringData<CantileverBracketPostConnectionBlock> weatheringData) {
        super(properties.mapColor(MapColor.METAL), weatheringData);

        this.registerDefaultState(defaultBlockState()
        );
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite()) {
            BlockPos supportPos = currentPos.relative(direction);
            BlockState supportState = level.getBlockState(supportPos);
            boolean supportIsPost = supportState.getBlock() instanceof IHorizontalExtensionConnectable conn && conn.postConnectionType(level, supportState, supportPos, state, currentPos) == EPostType.LATTICE;
            
            if (!supportIsPost) {
                state = ModBlocks.CANTILEVER_BRACKET.get(new ModBlocks.OxidizingKey(getWeatheringData().weatherState(), getWeatheringData().isWaxed())).getDefaultState()
                    .setValue(FACING, state.getValue(FACING))
                    .setValue(ROTATION, state.getValue(ROTATION))
                    .setValue(MULTIPART_SEGMENT, state.getValue(MULTIPART_SEGMENT))
                ;
            }
        }
        return state;
    }
}
