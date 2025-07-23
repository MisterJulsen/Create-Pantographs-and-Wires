package de.mrjulsen.paw.block;

import java.util.function.Supplier;

import de.mrjulsen.paw.block.abstractions.AbstractSimplePostBlock;
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

public class HBeamMastBlock extends AbstractSimplePostBlock implements IWeatheringBlock<HBeamMastBlock> {

    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

    private final WeatherState weatherState;
    private final Supplier<HBeamMastBlock> nextOxidationState;

    public HBeamMastBlock(Properties properties, WeatherState weatherState, Supplier<HBeamMastBlock> nextOxidationState) {
        super(properties.mapColor(MapColor.METAL));
        this.weatherState = weatherState;
        this.nextOxidationState = nextOxidationState;
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Vec2 getRotationPivotPoint(BlockState state) {
        return Vec2.ZERO;
    }

    @Override
    public EPostType postConnectionType(LevelReader level, BlockState state, BlockPos pos, BlockState extensionState, BlockPos extensionPos) {
        return EPostType.WALL;
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
    public Supplier<HBeamMastBlock> getNextState() {
        return nextOxidationState;
    }
}
