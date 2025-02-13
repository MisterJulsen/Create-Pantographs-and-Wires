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

public class ConcretePillarBlock extends AbstractSimplePostBlock {

    private static final VoxelShape SHAPE = Block.box(5.5d, 0, 5.5d, 10.5d, 16, 10.5d);
    private static final VoxelShape SHAPE_THICK = Block.box(4, 0, 4, 12, 16, 12);

    private final boolean thick;    

    public ConcretePillarBlock(Properties properties, boolean thick) {
        super(properties.mapColor(MapColor.METAL));
        this.thick = thick;
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
}
