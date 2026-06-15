package de.mrjulsen.paw.block.abstractions;

import com.simibubi.create.foundation.block.IBE;

import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.wires.graph.data.provider.BasicConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

public abstract class AbstractRotatableWireConnectorBlock<T extends WireConnectorBlockEntity> extends AbstractRotatableBlock implements IBE<T>, IWireConnector {

    public AbstractRotatableWireConnectorBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL).noOcclusion());
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    protected Vec3 transformWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData itemData, int index, IWireRenderDataCallback func) {
        if (!(state.getBlock() instanceof IWireConnector) || !(state.getBlock() instanceof IRotatableBlock rot)) {
            return Vec3.ZERO;
        }

        Vec2 pivot    = rot.getRotationPivotPoint(state);
        Vec2 rotPivot = rot.rotatedPivotPoint(state);
        Vec2 offset   = rot.getOffset(state);

        return VecHelper.rotate(
                        func.run(level, pos, state, itemData, index).subtract(pivot.x, 0, pivot.y),
                        getYRotation(state),
                        Axis.Y
                )
                .add(rotPivot.x, 0, rotPivot.y)
                .add(offset.x, 0, offset.y);
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        return new BasicConnectorDataProvider(
                transformWireAttachPoint(level, pos, level.getBlockState(pos), customData, connectionPointIndex, this::defaultWireAttachPoint)
                        .toVector3f()
        );
    }

    protected abstract Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData itemData, int index);

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        if (type != getBlockEntityType()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof WireConnectorBlockEntity wcbe) {
                wcbe.serverTick();
            }
        };
    }

    @FunctionalInterface
    protected interface IWireRenderDataCallback {
        Vec3 run(Level level, BlockPos pos, BlockState state, CustomData itemData, int index);
    }
}