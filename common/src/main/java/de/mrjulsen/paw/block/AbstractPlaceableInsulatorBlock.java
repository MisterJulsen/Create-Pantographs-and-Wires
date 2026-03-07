package de.mrjulsen.paw.block;

import java.util.Objects;

import de.mrjulsen.mcdragonlib.util.MapCache;
import de.mrjulsen.paw.block.abstractions.AbstractRotatableWireConnectorBlock;
import de.mrjulsen.paw.block.abstractions.AbstractRotatedConnectableBlock;
import de.mrjulsen.paw.registry.ModBlockEntities;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec2;

public abstract class AbstractPlaceableInsulatorBlock extends AbstractRotatableWireConnectorBlock<WireConnectorBlockEntity> {

    public static final String NBT_TENSION_WIRE_ATTACH_POINT = "TensionWireAttachPoint";

    public static final IntegerProperty MULTIPART_SEGMENT = AbstractRotatedConnectableBlock.MULTIPART_SEGMENT;
    protected final MapCache<Vec2, BlockState, BlockState> offsetCache = AbstractRotatedConnectableBlock.createOffsetCache();
    
    protected static record TransformationShapeKey(Direction direction, int rotation, BlockState state) {
        @Override
        public final boolean equals(Object other) {
            if (other instanceof TransformationShapeKey o) {
                return direction().equals(o.direction()) && rotation() == o.rotation();
            }
            return false;
        }
        @Override
        public final int hashCode() {
            return Objects.hash(direction(), rotation());
        }
    }

    public AbstractPlaceableInsulatorBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(defaultBlockState()
            .setValue(MULTIPART_SEGMENT, AbstractRotatedConnectableBlock.DEFAULT_SEGMENT)
        );
    }
    
    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(MULTIPART_SEGMENT);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
    
    @Override
    public Vec2 getOffset(BlockState state) {
        return offsetCache.get(state, state);
    }

    @Override
    public Class<WireConnectorBlockEntity> getBlockEntityClass() {
        return WireConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WireConnectorBlockEntity> getBlockEntityType() {
       return ModBlockEntities.WIRE_CONNECTOR_BLOCK_ENTITY.get();
    }
}
