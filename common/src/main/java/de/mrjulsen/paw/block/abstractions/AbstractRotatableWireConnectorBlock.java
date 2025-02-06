package de.mrjulsen.paw.block.abstractions;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VecHelper;

import de.mrjulsen.paw.util.Utils;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractRotatableWireConnectorBlock<T extends WireConnectorBlockEntity> extends AbstractRotatableBlock implements IBE<T>, IWireConnector {

    public AbstractRotatableWireConnectorBlock(Properties properties) {
        super(Properties.of().mapColor(MapColor.METAL)
        .noOcclusion());
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    /**
     * Rotates the wire attachment vector for the current block rotation.
     * @param level The level the connector is in
     * @param pos The position of the connector
     * @param state The connector state
     * @param itemData Additional item data
     * @param firstPoint Whether this is the first connector
     * @param func The function for the raw attach point calculation
     * @return The transformed vector of the given function
     */
    protected Vec3 transformWireAttachPoint(Level level, BlockPos pos, BlockState state, CompoundTag itemData, boolean firstPoint, IWireRenderDataCallback func) {
        if (state.getBlock() instanceof IWireConnector && state.getBlock() instanceof IRotatableBlock rot) {
            Vec2 pivot = rot.getRotationPivotPoint(state);
            Vec2 rotPivot = rot.rotatedPivotPoint(state);
            Vec2 offset = rot.getOffset(state);
            Vec3 result = VecHelper.rotate(func.run(level, pos, state, itemData, firstPoint).subtract(pivot.x, 0, pivot.y), getYRotation(state), Axis.Y)
                .add(rotPivot.x, 0, rotPivot.y)
                .add(offset.x, 0, offset.y)
            ;
            return result;
        }
        return Vec3.ZERO;
    }

    @Override
    public CompoundTag wireRenderData(Level level, BlockPos pos, BlockState state, CompoundTag itemData, boolean firstPoint) {
        CompoundTag nbt = new CompoundTag();
        Utils.putNbtVec3(nbt, IWireConnector.NBT_WIRE_ATTACH_POINT, transformWireAttachPoint(level, pos, state, itemData, firstPoint, this::defaultWireAttachPoint));
        return nbt;
    }
    
    /**
     * The relative coordinates where a wire should be attached to.
     * @param level The current level.
     * @param pos The pos of the connector block.
     * @param state The state of the connector block.
     * @param itemData Additional data stored in the wire item created while placing it.
     * @param firstPoint Whether this is the first or second connector block
     * @return The relative coordinates from the block's center.
     */
    protected abstract Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CompoundTag itemData, boolean firstPoint);
    
    @FunctionalInterface
    protected static interface IWireRenderDataCallback {
        Vec3 run(Level level, BlockPos pos, BlockState state, CompoundTag itemData, boolean firstPoint);
    }
}

