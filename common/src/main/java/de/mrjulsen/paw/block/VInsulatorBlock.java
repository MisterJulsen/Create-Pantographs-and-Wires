package de.mrjulsen.paw.block;

import de.mrjulsen.paw.util.Const;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.wires.item.WireBaseItem.CustomData;
import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.data.MapCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VInsulatorBlock extends AbstractPlaceableHangingInsulatorBlock {
    
    private static final VoxelShape SHAPE = Block.box(5, 2, 0, 11, 16, 16);
    private final MapCache<VoxelShape, TransformationShapeKey, TransformationShapeKey> shapesCache;

    public VInsulatorBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL)
            .noOcclusion()
        );
        this.shapesCache = new MapCache<>((key) -> {
            Direction direction = key.direction();
            VoxelShape result = ModMath.rotateShape(SHAPE, Axis.Y, (int)direction.getOpposite().toYRot());
    
            return result;
        }, TransformationShapeKey::hashCode, ECachingPriority.ALWAYS);
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        TransformationShapeKey key = new TransformationShapeKey(state.getValue(FACING), normalizedPropertyRotationIndex(state), state);
        return shapesCache.get(key, key);
    }

    @Override
    public Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData itemData, int index) {
        return new Vec3(0, Const.PIXEL * 2.5f, 0);
    }
}
