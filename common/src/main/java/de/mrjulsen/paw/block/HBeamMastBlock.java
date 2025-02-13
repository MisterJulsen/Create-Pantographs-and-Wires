package de.mrjulsen.paw.block;

import de.mrjulsen.paw.block.abstractions.AbstractSimplePostBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HBeamMastBlock extends AbstractSimplePostBlock {

    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

    public HBeamMastBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL));
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
}
