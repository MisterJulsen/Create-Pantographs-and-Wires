package de.mrjulsen.paw.blockentity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.joml.Vector2f;
import org.joml.Vector3f;

import de.mrjulsen.mcdragonlib.DragonLib;
import de.mrjulsen.mcdragonlib.client.model.ICustomModelBlockEntity;
import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.ModelContext.ModelProperty;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.MathUtils;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverInsulatorsPlacement;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock.ECantileverRegistrationArmType;
import de.mrjulsen.paw.block.property.ECantileverConnectionType;
import de.mrjulsen.paw.block.property.ECantileverMastConnection;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.event.ClientWrapper;
import de.mrjulsen.paw.registry.ModBlockEntities;
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

    public static record SubCantileverSetting(byte index, ECantileverRegistrationArmType registrationArm) {

        public static final SubCantileverSetting EMPTY = new SubCantileverSetting((byte)-1, ECantileverRegistrationArmType.CENTER);
        private static final String NBT_INDEX = "Index";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putByte(NBT_INDEX, index);
            nbt.putInt(NBT_REGISTRATION_ARM_TYPE, registrationArm.ordinal());
            return nbt;
        }

        public static SubCantileverSetting fromNbt(CompoundTag nbt) {
            return new SubCantileverSetting(
                nbt.getByte(NBT_INDEX),
                ECantileverRegistrationArmType.values()[nbt.getInt(NBT_REGISTRATION_ARM_TYPE)]
            );
        }
    }

    public static final byte MAX_CANTILEVERS = 3;
    public static final float Z_POS = 0.5f;
    public static final float Y_POS = DragonLib.PIXEL * 10;

    public static final ModelProperty<Float> PROPERTY_WIDTH = new ModelProperty<>();
    public static final ModelProperty<Float> PROPERTY_HEIGHT = new ModelProperty<>();
    public static final ModelProperty<ECantileverInsulatorsPlacement> PROPERTY_INSULATOR_PLACEMENT = new ModelProperty<>();
    public static final ModelProperty<ECantileverRegistrationArmType> PROPERTY_REGISTRATION_ARM = new ModelProperty<>();
    public static final ModelProperty<EInsulatorType> PROPERTY_INSULATOR_TYPE = new ModelProperty<>();
    public static final ModelProperty<Float> PROPERTY_CATENARY_HEIGHT = new ModelProperty<>();
    public static final ModelProperty<Byte> PROPERTY_CANTILEVERS_COUNT = new ModelProperty<>();
    public static final ModelProperty<ECantileverMastConnection> PROPERTY_MAST_CONNECTION_TYPE = new ModelProperty<>();
    public static final ModelProperty<CantileverData[]> PROPERTY_SUB_CANTILEVER_SETTINGS = new ModelProperty<>();
    public static final ModelProperty<Boolean> PROPERTY_USE_SUPPORT_TUBE = new ModelProperty<>();

    public static final String NBT_WIDTH = "Width";
    public static final String NBT_HEIGHT = "Height";
    public static final String NBT_INSULATOR_PLACEMENT = "InsulatorPlacement";
    public static final String NBT_REGISTRATION_ARM_TYPE = "RegistrationArmType";
    public static final String NBT_INSULATOR_TYPE = "InsulatorType";
    public static final String NBT_CATENARY_HEIGHT = "CatenaryHeight";
    public static final String NBT_USE_SUPPORT_TUBE = "UseSupportTube";
    public static final String NBT_POST_CONNECTION_OFFSET = "PostConnectionOffset";
    public static final String NBT_MAST_CONNECTION_TYPE = "MastConnectionType";
    public static final String NBT_CANTILEVERS_COUNT = "CantileversCount";
    public static final String NBT_SUB_CANTILEVER_SETTINGS = "SubCantileverSettings";

    public static final float DEFAULT_WIDTH = 2.5f;
    public static final float DEFAULT_HEIGHT = 1.5f;
    public static final ECantileverInsulatorsPlacement DEFAULT_INSULATOR_PLACEMENT = ECantileverInsulatorsPlacement.BACK;
    public static final ECantileverRegistrationArmType DEFAULT_REGISTRATION_ARM_TYPE = ECantileverRegistrationArmType.CENTER;
    public static final EInsulatorType DEFAULT_INSULATOR_TYPE = EInsulatorType.BROWN;
    public static final float DEFAULT_CATENARY_HEIGHT = 1;
    public static final boolean DEFAULT_USE_SUPPORT_TUBE = false;
    public static final float DEFAULT_POST_CONNECTION_OFFSET = 0;
    public static final ECantileverMastConnection DEFAULT_MAST_CONNECTION_TYPE = ECantileverMastConnection.NONE;
    public static final byte DEFAULT_CANTILEVERS_COUNT = 1;

    private float width = DEFAULT_WIDTH;
    private float height = DEFAULT_HEIGHT;
    private ECantileverInsulatorsPlacement insulatorPlacement = DEFAULT_INSULATOR_PLACEMENT;
    private ECantileverRegistrationArmType registrationArmType = DEFAULT_REGISTRATION_ARM_TYPE;
    private EInsulatorType insulatorType = DEFAULT_INSULATOR_TYPE;
    private float catenaryHeight = DEFAULT_CATENARY_HEIGHT;
    private boolean useSupportTube = DEFAULT_USE_SUPPORT_TUBE;
    private float postConnectionOffset = DEFAULT_POST_CONNECTION_OFFSET;
    private ECantileverMastConnection mastConnectionType = DEFAULT_MAST_CONNECTION_TYPE;
    private byte cantileversCount = DEFAULT_CANTILEVERS_COUNT;
    private final SubCantileverSetting[] subCanileverSettings;
    {
        subCanileverSettings = new SubCantileverSetting[MAX_CANTILEVERS - 1];
        Arrays.fill(subCanileverSettings, SubCantileverSetting.EMPTY);
    }

    private boolean doNotUpdate = false;

    public CantileverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putFloat(NBT_WIDTH, width);
        nbt.putFloat(NBT_HEIGHT, height);
        nbt.putInt(NBT_INSULATOR_PLACEMENT, insulatorPlacement.ordinal());
        nbt.putInt(NBT_REGISTRATION_ARM_TYPE, registrationArmType.ordinal());
        nbt.putFloat(NBT_INSULATOR_TYPE, insulatorType.ordinal());
        nbt.putFloat(NBT_CATENARY_HEIGHT, catenaryHeight);
        nbt.putFloat(NBT_POST_CONNECTION_OFFSET, postConnectionOffset);
        nbt.putByte(NBT_MAST_CONNECTION_TYPE, mastConnectionType.getIndex());
        nbt.putByte(NBT_CANTILEVERS_COUNT, cantileversCount);
        nbt.putBoolean(NBT_USE_SUPPORT_TUBE, useSupportTube);
        ListTag list = new ListTag();
        for (SubCantileverSetting s : subCanileverSettings) {
            list.add((s == null ? SubCantileverSetting.EMPTY : s).toNbt());
        }
        nbt.put(NBT_SUB_CANTILEVER_SETTINGS, list);
        
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.width = nbt.getFloat(NBT_WIDTH);
        this.height = nbt.getFloat(NBT_HEIGHT);
        this.insulatorPlacement = ECantileverInsulatorsPlacement.values()[nbt.getInt(NBT_INSULATOR_PLACEMENT)];
        this.registrationArmType = ECantileverRegistrationArmType.values()[nbt.getInt(NBT_REGISTRATION_ARM_TYPE)];
        this.insulatorType = EInsulatorType.values()[nbt.getInt(NBT_INSULATOR_TYPE)];
        this.catenaryHeight = nbt.getFloat(NBT_CATENARY_HEIGHT);
        this.postConnectionOffset = nbt.getFloat(NBT_POST_CONNECTION_OFFSET);
        this.mastConnectionType = ECantileverMastConnection.getByIndex(nbt.getByte(NBT_MAST_CONNECTION_TYPE));
        this.cantileversCount = MathUtils.clamp(nbt.getByte(NBT_CANTILEVERS_COUNT), (byte)1, MAX_CANTILEVERS);
        this.useSupportTube = nbt.getBoolean(NBT_USE_SUPPORT_TUBE);

        Arrays.fill(subCanileverSettings, SubCantileverSetting.EMPTY);
        for (Tag tag : nbt.getList(NBT_SUB_CANTILEVER_SETTINGS, Tag.TAG_COMPOUND)) {
            SubCantileverSetting s = SubCantileverSetting.fromNbt((CompoundTag)tag);
            byte idx = s.index();
            if (idx < 0 || idx >= subCanileverSettings.length) continue;
            this.subCanileverSettings[idx] = s;
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

    public record CantileverData(float x, float y, float z, float width, float height, float frontYOffset, ECantileverRegistrationArmType registrationArm, float catenaryHeight, float spacing) {
        public static final CantileverData EMPTY = new CantileverData(0, 0, 0, 0, 0, 0, ECantileverRegistrationArmType.CENTER, 0, 0);
    }

    private CantileverData[] calcCantilevers() {
        CantileverData[] dataArray = new CantileverData[getCantileversCount()];

        float spacing = (1.0f / getCantileversCount()) * 2;
        float z = Z_POS - (spacing * (getCantileversCount() - 1)) / 2f;

        List<Float> xPool = new ArrayList<>(getCantileversCount());
        List<Float> yPool = new ArrayList<>(getCantileversCount());
        for (float i = 0, x = -0.5f, y = 0.5f; i < getCantileversCount(); i++, x += 0.5f, y -= 0.5f) {
            xPool.add(x);
            yPool.add(y);
        }
        final Function<ECantileverRegistrationArmType, Vector2f> getter = (reg) -> {
            return switch (reg) {
                case INNER -> new Vector2f(xPool.remove(0), yPool.remove(0));
                case OUTER -> new Vector2f(xPool.remove(xPool.size() - 1), yPool.remove(yPool.size() - 1));
                default -> new Vector2f(
                    xPool.remove(MathUtils.clamp((int)Math.ceil(xPool.size() / 2f), 0, xPool.size() - 1)),
                    yPool.remove(MathUtils.clamp((int)Math.ceil(yPool.size() / 2f), 0, yPool.size() - 1))
                );
            };
        };

        for (int i = 0; i < getCantileversCount(); i++) {
            float lw = getWidth();
            float dY = 0;
            ECantileverRegistrationArmType lRegisterArm = getRegistrationArmType();
            float lCatenaryHeight = getCatenaryHeight();

            if (getCantileversCount() > 1 && subCanileverSettings != null) {
                if (i <= 0) {
                    Vector2f v = getter.apply(getRegistrationArmType());
                    lw += v.x();
                    dY += v.y();
                } else {
                    lRegisterArm = subCanileverSettings[i - 1].registrationArm();
                    Vector2f v = getter.apply(lRegisterArm);
                    lw += v.x();
                    dY += v.y();
                    if (getRegistrationArmType() != ECantileverRegistrationArmType.CENTER && subCanileverSettings[i - 1].registrationArm() == ECantileverRegistrationArmType.CENTER) {
                        lCatenaryHeight -= 0.5f;
                    }
                }
            }

            dataArray[i] = new CantileverData(DragonLib.PIXEL * ((16f - getPostConnectionOffset()) / 2), Y_POS, z, lw, getHeight(), dY, lRegisterArm, lCatenaryHeight, spacing);
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
        float y = cantilever.y();
        float yFront = y + cantilever.frontYOffset();
        float height = cantilever.height() - 0.5f;
        float xOffset = cantilever.z();
        Vector3f a = new Vector3f(xOffset - 0.5f, y, 0.5f - z);
        Vector3f b = new Vector3f(xOffset - 0.5f, yFront, 0.5f - size);
        Vector3f c = new Vector3f(xOffset - 0.5f, -height - 0.5f, 0.5f - z);

        Vec2 pivot = rot.getRotationPivotPoint(state);
        Vec2 rotPivot = rot.rotatedPivotPoint(state);
        Vec2 offset = rot.getOffset(state);
        UnaryOperator<Vector3f> transformFunc = (vec) -> {
            return ModMath.rotate(new Vector3f(vec).sub(pivot.x, 0, pivot.y), rot.getYRotation(state), Axis.Y)
                .add(rotPivot.x, 0, rotPivot.y)
                .add(offset.x, 0, offset.y);
        };

        return new CantileverShapeData(
            transformFunc.apply(a).add(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()),
            transformFunc.apply(b).add(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()),
            transformFunc.apply(c).add(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ())
        );
    }

    @Override
    public ModelContext getModelContext() {
        SubCantileverSetting[] sub = new SubCantileverSetting[subCanileverSettings.length];
        System.arraycopy(subCanileverSettings, 0, sub, 0, subCanileverSettings.length);
        return ModelContext.builder()
            .with(PROPERTY_WIDTH, width)
            .with(PROPERTY_HEIGHT, height)
            .with(PROPERTY_INSULATOR_PLACEMENT, insulatorPlacement)
            .with(PROPERTY_REGISTRATION_ARM, registrationArmType)
            .with(PROPERTY_INSULATOR_TYPE, insulatorType)
            .with(PROPERTY_CATENARY_HEIGHT, catenaryHeight)
            .with(PROPERTY_CANTILEVERS_COUNT, cantileversCount)
            .with(PROPERTY_MAST_CONNECTION_TYPE, mastConnectionType)
            .with(PROPERTY_SUB_CANTILEVER_SETTINGS, cantileverDataCache.get())
            .with(PROPERTY_USE_SUPPORT_TUBE, useSupportTube)
            .build();
    }
    
    @Override
    public void onBlockEntityLoad() {
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

    public ECantileverInsulatorsPlacement getInsulatorPlacement() {
        return insulatorPlacement;
    }

    public void setInsulatorPlacement(ECantileverInsulatorsPlacement insulatorPlacement) {
        this.insulatorPlacement = insulatorPlacement;
        update();
    }

    public ECantileverRegistrationArmType getRegistrationArmType() {
        return registrationArmType;
    }

    public void setRegistrationArmType(ECantileverRegistrationArmType registrationArmType) {
        this.registrationArmType = registrationArmType;
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
    
    public boolean shouldUseSupportTube() {
        return useSupportTube;
    }

    public void setUseSupportTube(boolean b) {
        this.useSupportTube = b;
        update();
    }

    public byte getCantileversCount() {
        return cantileversCount;
    }

    public void setCantileversCount(byte cantileversCount) {
        this.cantileversCount = cantileversCount;
        update();
    }

    public SubCantileverSetting[] getSubCanileverSettings() {
        return subCanileverSettings;
    }

    public void update() {
        notifyUpdate();
        updateModel();
        cantileverDataCache.clear();
    }
}
