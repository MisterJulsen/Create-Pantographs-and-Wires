package de.mrjulsen.paw.block.abstractions;

import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.client.gui.widgets.IIconEnum;
import de.mrjulsen.paw.registry.ModBlockEntities;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.paw.util.Utils;

import java.util.Arrays;

import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractCantileverBlock extends AbstractSupportedRotatableWireConnectorBlock<CantileverBlockEntity> implements ICatenaryWireConnector, IMultiblock {

    public static final float MIN_WIDTH = 1.5f;
    public static final float MAX_WIDTH = 6.5f;
    public static final float MIN_HEIGHT = 0f;
    public static final float MAX_HEIGHT = 3f;

    public static float getMaxHeight(float height) {
        return Math.max(1, Math.min(MAX_HEIGHT, height - 0.5f));
    }

    public static float getMinHeight(float height) {
        return Math.max(MIN_HEIGHT, Math.min(height - 0.5f, 1));
    }
    
    public static float getMaxWidth(float height) {
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, height + 3.5f));
    }

    public static float getMinWidth(float height) {
        return MIN_WIDTH;
    }

    public static enum ECantileverRegistrationArmType implements StringRepresentable, IIconEnum, ITranslatableEnum {
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

        @Override
        public String getEnumName() {
            return "cantilever_registration_arm";
        }

        @Override
        public String getEnumValueName() {
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
    }

    public static enum ECantileverInsulatorsPlacement implements StringRepresentable, IIconEnum, ITranslatableEnum {
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

        @Override
        public String getEnumName() {
            return "insulator_placement";
        }

        @Override
        public String getEnumValueName() {
            return name;
        }

        public static ECantileverInsulatorsPlacement def() {
            return BACK;
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

    protected TagKey<Block> getSupportBlockTag() {
        return ModBlocks.TAG_CANTILEVER_CONNECTABLE;
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double stretch = 16d * ((1d / Math.cos(Math.abs(Math.toRadians(getRelativeYRotation(state))))) - 1d);
        double a = 8 - 2;
        double b = 8 + 2;
        //if (level.getBlockEntity(pos) instanceof CantileverBlockEntity be) {
        //    double w = ((1.0f / be.getCantileversCount()) * 2) * (be.getCantileversCount() - 1) * 16;
        //    a -= w / 2;
        //    b += w / 2;
        //}
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
    public CompoundTag wireRenderData(Level level, BlockPos pos, BlockState state, CompoundTag itemData, int index) {
        CompoundTag nbt = super.wireRenderData(level, pos, state, itemData, index);
        Utils.putNbtVec3(nbt, NBT_TENSION_WIRE_ATTACH_POINT, transformWireAttachPoint(level, pos, state, itemData, index, this::tensionWireAttachPoint));
        return nbt;
    }
}
