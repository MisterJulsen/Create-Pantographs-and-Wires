package de.mrjulsen.wires.block;

import de.mrjulsen.wires.WireNetwork;
import de.mrjulsen.mcdragonlib.block.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class WireConnectorBlockEntity extends SyncedBlockEntity implements IBlockEntityExtension {

    private boolean wasUnloaded = false;

    public WireConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean wasUnloaded() {
        return wasUnloaded;
    }

    @Override
    public void onChunkUnloaded() {
        wasUnloaded = true;
    }

    @Override
    public void onBlockEntityLoad() {
        super.onLoad();
        wasUnloaded = false;
    }

	@Override
	public void setRemoved() {
		super.setRemoved();
        if (!wasUnloaded() && !level.isClientSide) {            
            WireNetwork.get(level).removeConnector(getLevel(), getBlockPos());
        }
        wasUnloaded = false;
	} 
}
