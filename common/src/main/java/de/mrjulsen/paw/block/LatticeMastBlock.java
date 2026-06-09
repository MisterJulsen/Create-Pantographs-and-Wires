package de.mrjulsen.paw.block;

import java.util.function.Supplier;

import de.mrjulsen.paw.block.abstractions.AbstractMultipartPostBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.abstractions.weathering.IAgingBlock;
import de.mrjulsen.paw.block.property.EPostPart;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class LatticeMastBlock extends AbstractMultipartPostBlock implements IWeatheringBlock<LatticeMastBlock> {

    private final VoxelShape BASE_SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    private final VoxelShape FOUNDATION_SHAPE = Shapes.or(BASE_SHAPE, Block.box(0.5d, -5, 0.5d, 15.5d, 4, 15.5d));

    private final WeatherData<LatticeMastBlock> weatheringData;

    public LatticeMastBlock(Properties properties, WeatherData<LatticeMastBlock> weatheringData) {
        super(properties
            .mapColor(MapColor.METAL)
        );

        this.weatheringData = weatheringData;
    }
    
    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == EPostPart.BOTTOM ? FOUNDATION_SHAPE : BASE_SHAPE;
    }

    @Override
    public EPostType postConnectionType(LevelReader level, BlockState state, BlockPos pos, BlockState cantileverState, BlockPos cantileverPos) {
        return EPostType.LATTICE;
    }

<<<<<<< HEAD
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.onRandomTick(state, level, pos, random);
    }

    public boolean isRandomlyTicking(BlockState state) {
        return getNext().isPresent();
    }

    @Override
    public @NotNull WeatherData<LatticeMastBlock> getWeatheringData() {
        return weatheringData;
    }

    @Override
=======
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.changeOverTime(state, level, pos, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return getNext().isPresent();
    }

    @Override
    public @NotNull WeatherData<LatticeMastBlock> getWeatheringData() {
        return weatheringData;
    }

    @Override
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
    public float getChanceModifier() {
        if (getWeatheringData().isWaxed()) return 0;
        return IWeatheringBlock.super.getChanceModifier();
    }
}
