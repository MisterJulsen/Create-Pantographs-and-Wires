package de.mrjulsen.paw.block.abstractions;

import de.mrjulsen.mcdragonlib.data.ITranslatableEnum;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.client.gui.widgets.IIconRepresentable;
import de.mrjulsen.paw.registry.ModBlockEntities;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.wires.graph.data.provider.CantileverConnectorDataProvider;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractCantileverBlock extends AbstractSupportedRotatableWireConnectorBlock<CantileverBlockEntity> implements ICatenaryWireConnector {

    public static final float MIN_WIDTH = 1.5f;
    public static final float MAX_WIDTH = 6.5f;
    public static final float MIN_HEIGHT = 0f;
    public static final float MAX_HEIGHT = 3f;
    public static final byte MAX_CANTILEVERS = 3;

    public record SliderConstraints(float min, float max, float step) {}
    public record Constraints(
        SliderConstraints width,
        SliderConstraints height,
        SliderConstraints armHeight,
        boolean registrationArmsAllowed
    ) {}

    public static Constraints calculate(float width, float height, float armHeight) {
        SliderConstraints widthConstraints = new SliderConstraints(MIN_WIDTH, MAX_WIDTH, 1.0f);

        float minHeight = MIN_HEIGHT;
        if (width >= 3) {
            int offset = (int) Math.floor((width - 1) / 2.0f);
            minHeight = Math.max(minHeight, offset * 0.5f);
        }

        if (width <= 1.5f && armHeight == 0.0f) {
            minHeight = Math.max(minHeight, 0.5f);
        }

        float maxHeight = MAX_HEIGHT;
        float minArmHeight = MIN_HEIGHT;

        if (height >= 2.0f) {
            minArmHeight = Math.max(minArmHeight, 1.0f);
        } else if (height >= 1.0f) {
            minArmHeight = Math.max(minArmHeight, 0.5f);
        }

        if (width <= 1.5f && height == 0.0f) {
            minArmHeight = Math.max(minArmHeight, 0.5f);
        }

        float maxArmHeight = Math.min(MAX_HEIGHT, Math.max(1.0f, height));
        float stepHeight = 0.5f;
        float stepArm = 0.5f;

        if (width <= 1.5f && height == 0.5f) {
            stepArm = 1.0f;
        }

        SliderConstraints heightConstraints = new SliderConstraints(minHeight, maxHeight, stepHeight);
        SliderConstraints armHeightConstraints = new SliderConstraints(minArmHeight, maxArmHeight, stepArm);
        boolean b = width > MIN_WIDTH;

        return new Constraints(widthConstraints, heightConstraints, armHeightConstraints, b);
    }

    public static byte additionalCantileversCheck(float width, float height, float catenaryHeight) {
        if (width <= 2) {
            return 1;
        }
        if (catenaryHeight < 0.5f) {
            return 2;
        }
        return MAX_CANTILEVERS;
    }

    public static enum ECantileverRegistrationArmType implements IIconRepresentable, ITranslatableEnum {
        CENTER("center", ModGuiIcons.CANTILEVER_CENTER, 0, 0),
        INNER("inner", ModGuiIcons.CANTILEVER_INNER, -0.25f, 1),
        OUTER("outer", ModGuiIcons.CANTILEVER_OUTER, 0.25f, 0);

        final String name;
        final ModGuiIcons icon;
        final float offset;
        final float registrationArmExtend;

        ECantileverRegistrationArmType(String name, ModGuiIcons icon, float offset, float registrationArmExtend) {
            this.name = name;
            this.icon = icon;
            this.offset = offset;
            this.registrationArmExtend = registrationArmExtend;
        }

        public static ECantileverRegistrationArmType getByName(String name) {
            return Arrays.stream(values()).filter(x -> x.getSerializedName().equals(name)).findFirst().orElse(CENTER);
        }

        @Override
        public ModGuiIcons getIcon() {
            return icon;
        }

        public float getOffset() {
            return offset;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static ECantileverRegistrationArmType def() {
            return CENTER;
        }

        public ECantileverRegistrationArmType opposite() {
            return switch (this) {
                case INNER -> OUTER;
                case OUTER -> INNER;
                default -> this;
            };
        }

        @Override
        public Data getTranslationData() {
            return new Data(PantographsAndWires.MOD_ID, "cantilever_registration_arm", name);
        }
    }

    public static enum ECantileverInsulatorsPlacement implements IIconRepresentable, ITranslatableEnum {
        BACK("back", ModGuiIcons.CANTILEVER_INSULATOR_BACK, 0),
        FRONT("front", ModGuiIcons.CANTILEVER_INSULATOR_FRONT, 0.8f);

        final String name;
        final ModGuiIcons icon;
        final float offsetFac;

        ECantileverInsulatorsPlacement(String name, ModGuiIcons icon, float offsetFac) {
            this.name = name;
            this.icon = icon;
            this.offsetFac = offsetFac;
        }

        public static ECantileverInsulatorsPlacement getByName(String name) {
            return Arrays.stream(values()).filter(x -> x.getSerializedName().equals(name)).findFirst().orElse(BACK);
        }

        @Override
        public ModGuiIcons getIcon() {
            return icon;
        }

        public float getPlacementOffsetFac() {
            return offsetFac;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static ECantileverInsulatorsPlacement def() {
            return BACK;
        }

        @Override
        public Data getTranslationData() {
            return new Data(PantographsAndWires.MOD_ID, "insulator_placement", name);
        }
    }

    public AbstractCantileverBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL)
            .noOcclusion()
        );
    }

    @Override
    public Class<CantileverBlockEntity> getBlockEntityClass() {
        return CantileverBlockEntity.class;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos relativePos = getSupportBlockPos(level, pos, state);
        BlockState supportState = level.getBlockState(relativePos);
        return super.canSurvive(state, level, pos) && (!(supportState.getBlock() instanceof ICantileverConnectableBlock c) || c.canCantileverConnect(level, relativePos, supportState, direction));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, Builder params) {
        BlockEntity blockEntity = (BlockEntity)params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof CantileverBlockEntity be) {
            byte count = be.getCantileversCount();
            List<ItemStack> items = new ArrayList<>();
            for (byte i = 0; i < count; i++) {
                items.addAll(super.getDrops(state, params));
            }
            return items;
        }
        return super.getDrops(state, params);
    }

    protected TagKey<Block> getSupportBlockTag() {
        return ModBlocks.TAG_CANTILEVER_CONNECTABLE;
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double stretch = 16d * ((1d / Math.cos(Math.abs(Math.toRadians(getRelativeYRotation(state))))) - 1d);
        double a = 8 - 2;
        double b = 8 + 2;
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(a, 0, 0, b, 16d, 16d + stretch);
            case WEST  -> Block.box(-stretch, 0, a, 16d, 16d, b);
            case EAST  -> Block.box(0, 0, a, 16d + stretch, 16d, b);
            default    -> Block.box(a, 0, -stretch, b, 16d, 16d);
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntityType<? extends CantileverBlockEntity> getBlockEntityType() {
       return ModBlockEntities.CANTILEVER_BLOCK_ENTITY.get();
    }

    @Override
    public Vec2 getRotationPivotPoint(BlockState state) {
        return new Vec2(0f, 1f);
    }

    @Override
    public ConnectorDataProvider getConnectorData(Level level, BlockPos pos, CustomData customData, int connectionPointIndex) {
        Vector3f contactAttachPoint = transformWireAttachPoint(level, pos, level.getBlockState(pos), customData, connectionPointIndex, this::defaultWireAttachPoint).toVector3f();
        Vector3f tensionattachPoint = transformWireAttachPoint(level, pos, level.getBlockState(pos), customData, connectionPointIndex, this::tensionWireAttachPoint).toVector3f();
        return new CantileverConnectorDataProvider(contactAttachPoint, tensionattachPoint);
    }
}
