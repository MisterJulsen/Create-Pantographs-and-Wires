package de.mrjulsen.paw.block;

import de.mrjulsen.paw.blockentity.MultiblockWireConnectorBlockEntity;
import de.mrjulsen.paw.item.CatenaryWireType;
import de.mrjulsen.paw.blockentity.IMultiblockBlockEntity;
import de.mrjulsen.paw.block.abstractions.AbstractSupportedRotatableWireConnectorBlock;
import de.mrjulsen.paw.block.abstractions.ICatenaryWireConnector;
import de.mrjulsen.paw.block.abstractions.IMultiblock;
import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import de.mrjulsen.paw.block.property.ECantileverConnectionType;
import de.mrjulsen.paw.registry.ModBlockEntities;
import de.mrjulsen.paw.registry.ModBlockTags;
import de.mrjulsen.paw.util.Const;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;

import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.config.ECachingPriority;
import de.mrjulsen.mcdragonlib.util.MapCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TensioningDeviceBlock extends AbstractSupportedRotatableWireConnectorBlock<MultiblockWireConnectorBlockEntity> implements ICatenaryWireConnector, IMultiblock {

    public static final int HEIGHT = 7;


    public static final BooleanProperty HELPER = BooleanProperty.create("helper");
    public static final EnumProperty<ECantileverConnectionType> CONNECTION = EnumProperty.create("connection", ECantileverConnectionType.class);

    private static final VoxelShape DEFAULT_SHAPE = Block.box(0.5d, 0, -0.25d, 15.5d, 16, 16);
    private static final MapCache<VoxelShape, BlockState, BlockState> shapesCache = new MapCache<>((state) -> {
        VoxelShape baseShape = ModMath.moveShape(DEFAULT_SHAPE, new Vec3(0, 0, Const.PIXEL * ((float)(16 - state.getValue(CONNECTION).getIndex()) / 2f)));
        Direction direction = state.getValue(FACING);
        VoxelShape shape = ModMath.rotateShape(baseShape, Axis.Y, (int)direction.getOpposite().toYRot());
        return shape;
    }, BlockState::hashCode, ECachingPriority.ALWAYS);


    public TensioningDeviceBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.defaultBlockState()
            .setValue(HELPER, false)
            .setValue(CONNECTION, ECantileverConnectionType.PX16)
        );
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {        
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(CONNECTION, HELPER);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return pState.getValue(HELPER) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPlaceContextExtension ext = (BlockPlaceContextExtension)context;
        Level level = context.getLevel();        
        MutableBlockPos refPos = new MutableBlockPos(ext.paw$getPlacedOnPos().getX(), ext.paw$getPlacedOnPos().getY(), ext.paw$getPlacedOnPos().getZ());

        BlockState refState = ext.paw$getPlacedOnState();
        ECantileverConnectionType refConnectionType = ECantileverConnectionType.getFirstForState(refState).orElse(ECantileverConnectionType.PX16);

        for (int i = 1; i < HEIGHT; i++) {
            refPos.move(0, -1, 0);
            BlockState supportState = level.getBlockState(refPos);
            ECantileverConnectionType connectionType = ECantileverConnectionType.getFirstForState(supportState).orElse(ECantileverConnectionType.PX16);
            if (refState.getBlock() != supportState.getBlock() && (refConnectionType != connectionType || (refConnectionType == ECantileverConnectionType.PX16 && !supportState.isFaceSturdy(level, refPos, context.getClickedFace())))) {
                return null;
            }
            BlockPos p = context.getClickedPos().relative(Direction.DOWN, i);
            if (!level.getBlockState(p).canBeReplaced(context) || level.isOutsideBuildHeight(p)) {
                return null;
            }
        }

        return super.getStateForPlacement(context)
            .setValue(CONNECTION, ECantileverConnectionType.getFirstForState(ext.paw$getPlacedOnState()).orElse(ECantileverConnectionType.PX16))
        ;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        MutableBlockPos refPos = new MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        for (int i = 1; i < HEIGHT; i++) {
            refPos.move(0, -1, 0);
            level.setBlock(refPos, state.setValue(HELPER, true), 0, 0);
            if (level.getBlockEntity(refPos) instanceof MultiblockWireConnectorBlockEntity be) {
                be.setOffset(1, i, 1);
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {        
        return canSurvive(state, level, currentPos) ? super.updateShape(state, direction, neighborState, level, currentPos, neighborPos) : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IMultiblockBlockEntity be) {
            int yOffset = be.getYOffset();
            MutableBlockPos mPos = new MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
            mPos.move(0, 1, 0);
            boolean b1 = yOffset <= 0 || (level.getBlockState(mPos).is(this) && level.getBlockEntity(mPos) instanceof IMultiblockBlockEntity b && b.getYOffset() == yOffset - 1);
            mPos.move(0, -2, 0);
            boolean b2 = yOffset >= HEIGHT - 1 || (level.getBlockState(mPos).is(this) && level.getBlockEntity(mPos) instanceof IMultiblockBlockEntity b && b.getYOffset() == yOffset + 1);
            
            if (!b1 || !b2) return false;

            mPos.move(0, 1, 0);
            BlockPos refPos = getSupportBlockPos(level, mPos, state);
            BlockState refState = level.getBlockState(refPos);
            ECantileverConnectionType refConnectionType = ECantileverConnectionType.getFirstForState(refState).orElse(ECantileverConnectionType.PX16);
            for (int i = yOffset; i < HEIGHT - 1; i++) {
                mPos.move(0, -1, 0);
                BlockPos supportPos = getSupportBlockPos(level, mPos, state);
                BlockState supportState = level.getBlockState(supportPos);
                ECantileverConnectionType connectionType = ECantileverConnectionType.getFirstForState(supportState).orElse(ECantileverConnectionType.PX16);
                if (refState.getBlock() != supportState.getBlock() && (refConnectionType != connectionType || (refConnectionType == ECantileverConnectionType.PX16 && !supportState.isFaceSturdy(level, supportPos, state.getValue(FACING).getOpposite())))) {
                    return false;
                }
            }
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected TagKey<Block> getSupportBlockTag() {
        return ModBlockTags.TENSIONING_DEVICE_CONNECTABLE;
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapesCache.get(state, state);
    }

    @Override
    public Vec3 defaultWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData itemData, int index) {
        return new Vec3(Const.PIXEL * 3.5f - 0.5f, Const.PIXEL * 0.25, Const.PIXEL * 8.25f - 0.5f + (Const.PIXEL * (float)((16 - state.getValue(CONNECTION).getIndex()) / 2f)));
    }

    @Override
    public Vec3 tensionWireAttachPoint(Level level, BlockPos pos, BlockState state, CustomData itemData, int index) {
        return new Vec3(Const.PIXEL * 12.5f - 0.5f, Const.PIXEL * 10.25f, Const.PIXEL * 8.25f - 0.5f + (Const.PIXEL * (float)((16 - state.getValue(CONNECTION).getIndex()) / 2f)));
    }
    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        Vector3f contactAttachPoint = transformWireAttachPoint(level, pos, level.getBlockState(pos), customData, connectionPointIndex, this::defaultWireAttachPoint).toVector3f();
        Vector3f tensionattachPoint = transformWireAttachPoint(level, pos, level.getBlockState(pos), customData, connectionPointIndex, this::tensionWireAttachPoint).toVector3f();
        CompoundTag customNbt = customData.getCommonData();
        customNbt.putBoolean(CatenaryWireType.NBT_SUPER_TIGHTENED, true);
        customData.setCommonData(customNbt);
        return new CantileverConnectorDataProvider(contactAttachPoint, tensionattachPoint);
    }

    @Override
    public Vec2 getRotationPivotPoint(BlockState state) {
        return new Vec2(0f, 1f);
    }

    @Override
    public Vec3 multiblockSize() {
        return new Vec3(1, HEIGHT, 1);
    }
    
    @Override
    public boolean canConnectWire(LevelReader level, BlockPos pos, BlockState state) {
        return !state.getValue(HELPER);
    }

    @Override
    public Class<MultiblockWireConnectorBlockEntity> getBlockEntityClass() {
        return MultiblockWireConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends MultiblockWireConnectorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.MULTIBLOCK_WIRE_CONNECTOR_BLOCK_ENTITY.get();
    }
}
