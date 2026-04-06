package de.mrjulsen.paw.block;

import de.mrjulsen.paw.block.abstractions.AbstractMultipartPostBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ConcretePillarBlock extends AbstractMultipartPostBlock implements IWeatheringBlock<ConcretePillarBlock> {

    private static final VoxelShape SHAPE = Block.box(5.5d, 0, 5.5d, 10.5d, 16, 10.5d);
    private static final VoxelShape SHAPE_THICK = Block.box(4, 0, 4, 12, 16, 12);

    private final boolean thick;
    private final IWeatheringBlock.WeatherData<ConcretePillarBlock> weatheringData;

    public ConcretePillarBlock(Properties properties, IWeatheringBlock.WeatherData<ConcretePillarBlock> weatheringData, boolean thick) {
        super(properties.mapColor(MapColor.METAL));
        this.thick = thick;
        this.weatheringData = weatheringData;
    }

    public static ConcretePillarBlock tickPillar(Properties properties, IWeatheringBlock.WeatherData<ConcretePillarBlock> weatheringData) {
        return new  ConcretePillarBlock(properties, weatheringData, true);
    }

    public static ConcretePillarBlock post(Properties properties, IWeatheringBlock.WeatherData<ConcretePillarBlock> weatheringData) {
        return new  ConcretePillarBlock(properties, weatheringData, false);
    }

    public boolean isThick() {
        return thick;
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return isThick() ? SHAPE_THICK : SHAPE;
    }

    @Override
    public Vec2 getRotationPivotPoint(BlockState state) {
        return Vec2.ZERO;
    }

    @Override
    public EPostType postConnectionType(LevelReader level, BlockState state, BlockPos pos, BlockState cantileverState, BlockPos cantileverPos) {
        return thick ? EPostType.WALL : EPostType.FENCE;
    }

    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.changeOverTime(state, level, pos, random);
    }

    public boolean isRandomlyTicking(BlockState state) {
        return getNext().isPresent();
    }

    @Override
    public @NotNull IWeatheringBlock.WeatherData<ConcretePillarBlock> getWeatheringData() {
        return weatheringData;
    }

    @Override
    public float getChanceModifier() {
        if (getWeatheringData().isWaxed()) return 0;
        return IWeatheringBlock.super.getChanceModifier();
    }
}
