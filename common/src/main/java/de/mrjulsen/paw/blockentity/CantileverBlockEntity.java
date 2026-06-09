package de.mrjulsen.paw.blockentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.HolderLookup;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.model.ICustomModelBlockEntity;
import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.ModelContext.ModelProperty;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.block.property.ECantileverMastConnection;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.event.ClientWrapper;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.paw.util.ModMath;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

public class CantileverBlockEntity extends WireConnectorBlockEntity implements ICustomModelBlockEntity {

    public static final int VERSION = 1;

    public static final float DEFAULT_WIDTH = 2.5f;
    public static final float DEFAULT_HEIGHT = 1.5f;
    public static final float DEFAULT_Y_OFFSET = 0;
    public static final ECantileverInsulatorsPlacement DEFAULT_INSULATOR_PLACEMENT = ECantileverInsulatorsPlacement.BACK;
    public static final ECantileverRegistrationArmType DEFAULT_REGISTRATION_ARM_TYPE = ECantileverRegistrationArmType.CENTER;
    public static final EInsulatorType DEFAULT_INSULATOR_TYPE = EInsulatorType.BROWN;
    public static final boolean DEFAULT_SHOW_BRACING = false;
    public static final float DEFAULT_CATENARY_HEIGHT = 1;
    public static final float DEFAULT_POST_CONNECTION_OFFSET = 0;
    public static final ECantileverMastConnection DEFAULT_MAST_CONNECTION_TYPE = ECantileverMastConnection.NONE;

    public static final byte MAX_CANTILEVERS = 3;
    public static final float Y_POS = DragonLib.BLOCK_PIXEL * 10;
    public static final float Z_POS = 0.5f;
    public static final float CANTILEVER_D = 0.5f;

    public static record SubCantileverSetting(
            ECantileverRegistrationArmType registrationArm,
            ECantileverInsulatorsPlacement insulatorPlacement,
            boolean showBracing
    ) {
        public static final SubCantileverSetting EMPTY = new SubCantileverSetting(
                DEFAULT_REGISTRATION_ARM_TYPE,
                DEFAULT_INSULATOR_PLACEMENT,
                DEFAULT_SHOW_BRACING
        );

        private static final String NBT_INDEX = "Index";
        public static final String NBT_INSULATOR_PLACEMENT = "InsulatorPlacement";
        public static final String NBT_REGISTRATION_ARM_TYPE = "RegistrationArmType";
        public static final String NBT_SHOW_BRACING = "ShowBracing";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt(NBT_REGISTRATION_ARM_TYPE, registrationArm.ordinal());
            nbt.putInt(NBT_INSULATOR_PLACEMENT, insulatorPlacement.ordinal());
            nbt.putBoolean(NBT_SHOW_BRACING, showBracing);
            return nbt;
        }

        public static SubCantileverSetting fromNbt(CompoundTag nbt) {
            return new SubCantileverSetting(
                    ECantileverRegistrationArmType.values()[nbt.getInt(NBT_REGISTRATION_ARM_TYPE)],
                    ECantileverInsulatorsPlacement.values()[nbt.getInt(NBT_INSULATOR_PLACEMENT)],
                    nbt.getBoolean(NBT_SHOW_BRACING)
            );
        }
    }


    public static final ModelProperty<EInsulatorType> PROPERTY_INSULATOR_TYPE = new ModelProperty<>();
    public static final ModelProperty<ECantileverMastConnection> PROPERTY_MAST_CONNECTION_TYPE = new ModelProperty<>();
    public static final ModelProperty<CantileverData[]> PROPERTY_SUB_CANTILEVER_SETTINGS = new ModelProperty<>();

    public static final String NBT_VERSION = "Version";
    public static final String NBT_WIDTH = "Width";
    public static final String NBT_HEIGHT = "Height";
    public static final String NBT_Y_OFFSET = "YOffset";
    public static final String NBT_INSULATOR_TYPE = "InsulatorType";
    public static final String NBT_CATENARY_HEIGHT = "CatenaryHeight";
    public static final String NBT_POST_CONNECTION_OFFSET = "PostConnectionOffset";
    public static final String NBT_MAST_CONNECTION_TYPE = "MastConnectionType";
    public static final String NBT_SUB_CANTILEVER_SETTINGS = "SubCantileverSettings";

    private float width = DEFAULT_WIDTH;
    private float height = DEFAULT_HEIGHT;
    private float yOffset = DEFAULT_Y_OFFSET;
    private EInsulatorType insulatorType = DEFAULT_INSULATOR_TYPE;
    private float catenaryHeight = DEFAULT_CATENARY_HEIGHT;
    private float postConnectionOffset = DEFAULT_POST_CONNECTION_OFFSET;
    private ECantileverMastConnection mastConnectionType = DEFAULT_MAST_CONNECTION_TYPE;
    private final List<SubCantileverSetting> subCanileverSettings = new ArrayList<>(MAX_CANTILEVERS);

    private boolean doNotUpdate = false;

    public CantileverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putFloat(NBT_WIDTH, width);
        nbt.putFloat(NBT_HEIGHT, height);
        nbt.putFloat(NBT_Y_OFFSET, yOffset);
        nbt.putFloat(NBT_INSULATOR_TYPE, insulatorType.ordinal());
        nbt.putFloat(NBT_CATENARY_HEIGHT, catenaryHeight);
        nbt.putFloat(NBT_POST_CONNECTION_OFFSET, postConnectionOffset);
        nbt.putByte(NBT_MAST_CONNECTION_TYPE, mastConnectionType.getIndex());
        ListTag list = new ListTag();
        for (SubCantileverSetting s : subCanileverSettings) {
            list.add((s == null ? SubCantileverSetting.EMPTY : s).toNbt());
        }
        nbt.put(NBT_SUB_CANTILEVER_SETTINGS, list);
        
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.width = nbt.getFloat(NBT_WIDTH);
        this.height = nbt.getFloat(NBT_HEIGHT);
        this.yOffset = nbt.getFloat(NBT_Y_OFFSET);
        this.insulatorType = EInsulatorType.values()[nbt.getInt(NBT_INSULATOR_TYPE)];
        this.catenaryHeight = nbt.getFloat(NBT_CATENARY_HEIGHT);
        this.postConnectionOffset = nbt.getFloat(NBT_POST_CONNECTION_OFFSET);
        this.mastConnectionType = ECantileverMastConnection.getByIndex(nbt.getByte(NBT_MAST_CONNECTION_TYPE));

        this.subCanileverSettings.clear();
        if (nbt.contains(SubCantileverSetting.NBT_INSULATOR_PLACEMENT) && nbt.contains(SubCantileverSetting.NBT_REGISTRATION_ARM_TYPE)) {
            this.subCanileverSettings.add(new SubCantileverSetting(
                    ECantileverRegistrationArmType.values()[nbt.getInt(SubCantileverSetting.NBT_REGISTRATION_ARM_TYPE)],
                    ECantileverInsulatorsPlacement.values()[nbt.getInt(SubCantileverSetting.NBT_INSULATOR_PLACEMENT)],
                    nbt.getBoolean(SubCantileverSetting.NBT_SHOW_BRACING)
            ));
        }
        List<Tag> tags = nbt.getList(NBT_SUB_CANTILEVER_SETTINGS, Tag.TAG_COMPOUND);
        byte maxCount = nbt.contains("CantileversCount") ? nbt.getByte("CantileversCount") : Byte.MAX_VALUE;
        for (int i = 0; i < tags.size() && i < maxCount - 1; i++) {
            CompoundTag tag = (CompoundTag)tags.get(i);
            SubCantileverSetting s = SubCantileverSetting.fromNbt(tag);
            subCanileverSettings.add(s);
        }
        updateModel();
        cantileverDataCache.clear();
    }

    public void setDoNotUpdate(boolean b) {
        this.doNotUpdate = b;
    }

    public boolean shouldNotUpdate() {
        return doNotUpdate;
    }

    public record CantileverData(float x, float y, float z, float width, float height, float frontYOffset, float catenaryHeight, float spacing, SubCantileverSetting settings) {
        public static final CantileverData EMPTY = of(SubCantileverSetting.EMPTY);

        public static CantileverData of(SubCantileverSetting settings) {
            return new CantileverData(0, 0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, 0, DEFAULT_CATENARY_HEIGHT, 0, settings);
        }

        public static CantileverData simple(float yOffset, float width, float height, float catenaryHeight, SubCantileverSetting settings) {
            return new CantileverData(0, yOffset, 0, width, height, 0, catenaryHeight, 0, settings);
        }
    }

    private CantileverData[] calcCantilevers() {
        List<SubCantileverSetting> settings = new ArrayList<>(this.subCanileverSettings);
        if (settings.isEmpty()) {
            settings.add(SubCantileverSetting.EMPTY);
        }
        CantileverData[] dataArray = new CantileverData[settings.size()];

        float spacing = (1.0f / dataArray.length) * 2;
        float z = Z_POS - (spacing * (dataArray.length - 1)) / 2f;

        List<Float> xPool = new ArrayList<>(dataArray.length);
        List<Float> yPool = new ArrayList<>(dataArray.length);

        for (float i = 0; i < dataArray.length; i++) {
            xPool.add((i - (dataArray.length - 1) / 2f) * CANTILEVER_D);
            yPool.add((i - 1) * -CANTILEVER_D);
        }
        final Function<ECantileverRegistrationArmType, Vector2f> getter = (reg) ->
            switch (reg) {
                case INNER -> new Vector2f(xPool.remove(0), yPool.remove(0));
                case OUTER -> new Vector2f(xPool.remove(xPool.size() - 1), yPool.remove(yPool.size() - 1));
                default -> new Vector2f(
                    xPool.remove(MathUtils.clamp((int)Math.ceil(xPool.size() / 2f), 0, xPool.size() - 1)),
                    yPool.remove(MathUtils.clamp((int)Math.ceil(yPool.size() / 2f), 0, yPool.size() - 1))
                );
            }
        ;

        for (byte i = 0; i < dataArray.length; i++) {
            SubCantileverSetting setting = settings.get(i);
            float w = getWidth();
            float dY = 0;
            ECantileverRegistrationArmType registerArm = setting.registrationArm();
            float catenaryHeight = getCatenaryHeight();

            if (dataArray.length > 1) {
                if (i <= 0) {
                    Vector2f v = getter.apply(registerArm);
                    w += v.x();
                    dY += v.y();
                } else {
                    Vector2f v = getter.apply(registerArm);
                    w += v.x();
                    dY += v.y();
                    if (registerArm == ECantileverRegistrationArmType.CENTER && settings.get(0).registrationArm() != ECantileverRegistrationArmType.CENTER) {
                        catenaryHeight -= 0.5f;
                    }
                }
            }

            dataArray[i] = new CantileverData(
                    DragonLib.BLOCK_PIXEL * ((16f - getPostConnectionOffset()) / 2),
                    yOffset,
                    z,
                    w,
                    getHeight(),
                    dY,
                    catenaryHeight,
                    spacing,
                    new SubCantileverSetting(
                            registerArm,
                            setting.insulatorPlacement(),
                            setting.showBracing()
                    )
            );
            z += spacing;
        }
        return dataArray;
    }

    public final Cache<CantileverData[]> cantileverDataCache = new Cache<>(this::calcCantilevers);

    public CantileverData[] getCantileverData() {
        return cantileverDataCache.get();
    }

    public record CantileverShapeData(Vector3f stayTubeRoot, Vector3f front, Vector3f bracketTubeRoot) {}

    public CantileverShapeData getCantileverInteractionShape(int cantileverIndex) {
        CantileverData cantilever = getCantileverData()[cantileverIndex];
        BlockState state = getLevel().getBlockState(getBlockPos());
        if (!(state.getBlock() instanceof CantileverBlock rot)) {
            return new CantileverShapeData(new Vector3f(), new Vector3f(), new Vector3f());
        }

        float size = cantilever.width();
        float z = -cantilever.x();
        float y = Y_POS - cantilever.y();
        float yFront = y + cantilever.frontYOffset();
        float height = cantilever.height() - 0.5f + cantilever.y();
        float xOffset = cantilever.z();
        Vector3f a = new Vector3f(xOffset - 0.5f, y, 0.5f - z);
        Vector3f b = new Vector3f(xOffset - 0.5f, yFront, 0.5f - size);
        Vector3f c = new Vector3f(xOffset - 0.5f, -height - 0.5f, 0.5f - z);

        Vec2 pivot = rot.getRotationPivotPoint(state);
        Vec2 rotPivot = rot.rotatedPivotPoint(state);
        Vec2 offset = rot.getOffset(state);
        UnaryOperator<Vector3f> transformFunc = (vec) -> {
            Vector3d v = ModMath.rotate(new Vector3d(vec).sub(pivot.x, 0, pivot.y), rot.getYRotation(state), Axis.Y)
                .add(rotPivot.x, 0, rotPivot.y)
                .add(offset.x, 0, offset.y);
            return new Vector3f((float)v.x(), (float)v.y(), (float)v.z());
        };

        return new CantileverShapeData(
            transformFunc.apply(a).add(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()),
            transformFunc.apply(b).add(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()),
            transformFunc.apply(c).add(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ())
        );
    }

    @Override
    public ModelContext getModelContext() {
        return ModelContext.builder()
                .with(PROPERTY_INSULATOR_TYPE, insulatorType)
                .with(PROPERTY_MAST_CONNECTION_TYPE, mastConnectionType)
                .with(PROPERTY_SUB_CANTILEVER_SETTINGS, cantileverDataCache.get())
                .build();
    }

    @Override
    public void dragonlib$onBlockEntityLoad() {
        if (getLevel() != null && getLevel().getBlockState(getBlockPos()).getBlock() instanceof AbstractCantileverBlock cantilever) {
            BlockState support = getLevel().getBlockState(cantilever.getSupportingBlockPos(getBlockState(), level, getBlockPos()));
            setPostConnectionOffset(ModBlocks.getFirstCantileverConnectionTagForState(support));
            setMastConnectionType(ECantileverMastConnection.getFirstForState(support));
        }
    }

    private void updateModel() {
        if (getLevel() != null && getLevel().isClientSide()) {
            ClientWrapper.setSectionDirty(SectionPos.of(getBlockPos()));
        }
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
        update();
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
        update();
    }

    public EInsulatorType getInsulatorType() {
        return insulatorType;
    }

    public void setMastConnectionType(ECantileverMastConnection mastConnection) {
        this.mastConnectionType = mastConnection;
        update();
    }

    public ECantileverMastConnection getMastConnectionType() {
        return mastConnectionType;
    }

    public void setInsulatorType(EInsulatorType insulatorType) {
        this.insulatorType = insulatorType;
        update();
    }

    public float getCatenaryHeight() {
        return catenaryHeight;
    }

    public void setCatenaryHeight(float catenaryHeight) {
        this.catenaryHeight = catenaryHeight;
        update();
    }

    public float getPostConnectionOffset() {
        return postConnectionOffset;
    }

    public void setPostConnectionOffset(float postConnectionOffset) {
        this.postConnectionOffset = postConnectionOffset;
        update();
    }

    public float getYOffset() {
        return yOffset;
    }

    public List<SubCantileverSetting> getSubCantileverSettings() {
        return subCanileverSettings;
    }

    public void update() {
        notifyUpdate();
        updateModel();
        cantileverDataCache.clear();
    }
}
