package de.mrjulsen.wires.block;

import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.NodeId;

import java.util.Optional;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class WireConnectorBlockEntity extends SyncedBlockEntity implements IBlockEntityExtension {

    private static final String NBT_NODE_ID = "NodeId";

    private boolean wasUnloaded = false;
    private NodeId nodeId;

    public WireConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (nodeId != null) tag.put(NBT_NODE_ID, nodeId.toNbt());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_NODE_ID)) this.nodeId = NodeId.fromNbt(tag.getCompound(NBT_NODE_ID));
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
            if (nodeId != null) {
                WireGraphManager.get(level, nodeId.graphId()).removeNode(nodeId.id(), new Vector3d(getBlockPos().getX() + 0.5f, getBlockPos().getY() + 0.5f, getBlockPos().getZ() + 0.5f), Optional.empty());
            }
        }
        wasUnloaded = false;
	} 

    public NodeId getNodeId() {
        return nodeId;
    }

    public void setNodeId(NodeId id) {
        this.nodeId = id;
        notifyUpdate();
    }
}
