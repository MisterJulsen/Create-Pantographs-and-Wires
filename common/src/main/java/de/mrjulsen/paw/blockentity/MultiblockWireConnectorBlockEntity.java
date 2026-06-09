package de.mrjulsen.paw.blockentity;

import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import de.mrjulsen.mcdragonlib.util.math.MathUtils;
import de.mrjulsen.paw.block.abstractions.AbstractRotatableBlock;
import de.mrjulsen.paw.block.abstractions.IMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MultiblockWireConnectorBlockEntity extends WireConnectorBlockEntity implements IMultiblockBlockEntity {

    private static final String NBT_X_SIZE = "XSize";
    private static final String NBT_Y_SIZE = "YSize";
    private static final String NBT_Z_SIZE = "ZSize";

    private final int maxXSize;
    private final int maxYSize;
    private final int maxZSize;

    private int xOffset;
    private int yOffset;
    private int zOffset;

    private BlockPos rootPos;

    public MultiblockWireConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (getBlockState().getBlock() instanceof IMultiblock mb) {
            Vec3 vec = mb.multiblockSize();
            maxXSize = (int)vec.x;
            maxYSize = (int)vec.y;
            maxZSize = (int)vec.z;
        } else {
            maxXSize = 1;
            maxYSize = 1;
            maxZSize = 1;
        }
    }

    public void setRootPos(BlockPos pos) {
        this.rootPos = pos;
    }

    public BlockPos getRootPos() {
        return rootPos;
    }

    public int getDistanceToRoot() {
        Direction direction = getBlockState().getValue(AbstractRotatableBlock.FACING);
        return direction.getAxis() == Axis.X ? Math.abs(rootPos.getX() - getBlockPos().getX()) : Math.abs(rootPos.getZ() - getBlockPos().getZ());
    }

    public int getYDistanceToRoot() {
        return Math.abs(rootPos.getY() - getBlockPos().getY());
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putInt(NBT_X_SIZE, xOffset);
        nbt.putInt(NBT_Y_SIZE, yOffset);
        nbt.putInt(NBT_Z_SIZE, zOffset);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        xOffset = MathUtils.clamp(nbt.getInt(NBT_X_SIZE), 0, maxXSize - 1);
        yOffset = MathUtils.clamp(nbt.getInt(NBT_Y_SIZE), 0, maxYSize - 1);
        zOffset = MathUtils.clamp(nbt.getInt(NBT_Z_SIZE), 0, maxZSize - 1);
    }

    @Override
    public int getXOffset() {
        return xOffset;
    }

    @Override
    public int getYOffset() {
        return yOffset;
    }

    @Override
    public int getZOffset() {
        return zOffset;
    }

    @Override
    public int getXSize() {
        return maxXSize;
    }

    @Override
    public int getYSize() {
        return maxYSize;
    }

    @Override
    public int getZSize() {
        return maxZSize;
    }

    @Override
    public void setOffset(int x, int y, int z) {
        this.xOffset = MathUtils.clamp(x, 0, maxXSize - 1);
        this.yOffset = MathUtils.clamp(y, 0, maxYSize - 1);
        this.zOffset = MathUtils.clamp(z, 0, maxZSize - 1);
        notifyUpdate();
    }    
}
