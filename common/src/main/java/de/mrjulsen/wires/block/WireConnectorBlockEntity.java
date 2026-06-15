package de.mrjulsen.wires.block;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import de.mrjulsen.mcdragonlib.block.IBlockEntityExtension;
import de.mrjulsen.paw.block.ConnectorBlock;
import de.mrjulsen.paw.item.PowerWireType;
import de.mrjulsen.wires.graph.WireEdge;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import de.mrjulsen.wires.util.NodeId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import org.joml.Vector3d;

import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;

public class WireConnectorBlockEntity extends SyncedBlockEntity implements IBlockEntityExtension, IEnergyStorage {

    private static final String NBT_NODE_ID = "NodeId";
    private static final String NBT_ENERGY = "Energy";

    private static final int MAX_ENERGY = 10000;
    private static final int MAX_TRANSFER = 1000;

    private boolean wasUnloaded = false;
    private NodeId nodeId;
    private int energy;

    public WireConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (nodeId != null) tag.put(NBT_NODE_ID, nodeId.toNbt());
        tag.putInt(NBT_ENERGY, energy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(NBT_NODE_ID)) this.nodeId = NodeId.fromNbt(tag.getCompound(NBT_NODE_ID));
        energy = tag.getInt(NBT_ENERGY);
    }

    public boolean wasUnloaded() {
        return wasUnloaded;
    }

    @Override
    public void dragonlib$onChunkUnloaded() {
        wasUnloaded = true;
    }

    @Override
    public void dragonlib$onBlockEntityLoad() {
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

    public void serverTick() {
        if (level != null && !level.isClientSide) {
            transferEnergyThroughWires();
            if (isConnector()) {
                transferEnergyWithNeighbors();
            }
        }
    }

    private boolean isConnector() {
        return level != null && getBlockState().getBlock() instanceof ConnectorBlock;
    }

    private void transferEnergyThroughWires() {
        if (energy <= 0) return;

        NodeId nodeId = getNodeId();
        if (nodeId == null) return;

        WireGraph graph = WireGraphManager.get(level, nodeId.graphId());
        if (graph == null) return;

        WireNode startNode = graph.getNode(nodeId.id());
        if (startNode == null) return;

        Set<WireConnectorBlockEntity> targets = new HashSet<>();
        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new LinkedList<>();

        queue.add(nodeId.id());
        visited.add(nodeId.id());

        while (!queue.isEmpty()) {
            UUID currentId = queue.poll();
            WireNode current = graph.getNode(currentId);
            if (current == null) continue;

            for (UUID edgeId : current.getConnections()) {
                WireEdge edge = graph.getEdge(edgeId);
                if (edge == null) continue;
                if (!(edge.getType() instanceof PowerWireType)) continue;

                UUID nextId = edge.getNodeAId().equals(currentId) ? edge.getNodeBId() : edge.getNodeAId();
                if (visited.contains(nextId)) continue;
                visited.add(nextId);

                WireNode nextNode = graph.getNode(nextId);
                if (nextNode == null) continue;

                BlockPos nextPos = new BlockPos(
                    (int) nextNode.getPos().x(),
                    (int) nextNode.getPos().y(),
                    (int) nextNode.getPos().z()
                );
                BlockEntity be = level.getBlockEntity(nextPos);

                if (be instanceof WireConnectorBlockEntity wcbe && wcbe != this) {
                    int space = wcbe.getMaxEnergyStored() - wcbe.getEnergyStored();
                    if (space > 0) {
                        targets.add(wcbe);
                    }
                    queue.add(nextId);
                } else {
                    queue.add(nextId);
                }
            }
        }

        if (targets.isEmpty()) return;

        int energyPerTarget = Math.min(MAX_TRANSFER, energy / targets.size());
        for (WireConnectorBlockEntity target : targets) {
            int space = target.getMaxEnergyStored() - target.getEnergyStored();
            int transfer = Math.min(energyPerTarget, space);
            if (transfer > 0) {
                this.energy -= transfer;
                target.energy += transfer;
                target.notifyUpdate();
            }
        }
        notifyUpdate();
    }

    private void transferEnergyWithNeighbors() {
        if (level == null) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = getBlockPos().relative(dir);
            IEnergyStorage neighborStorage = level.getCapability(
                Capabilities.EnergyStorage.BLOCK,
                neighborPos,
                dir.getOpposite()
            );
            if (neighborStorage == null) continue;

            if (energy > 0 && neighborStorage.canReceive()) {
                int accepted = neighborStorage.receiveEnergy(Math.min(MAX_TRANSFER, energy), false);
                if (accepted > 0) {
                    energy -= accepted;
                    notifyUpdate();
                }
            }
        }
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!isConnector()) return 0;
        int received = Math.min(MAX_ENERGY - energy, Math.min(MAX_TRANSFER, maxReceive));
        if (!simulate) {
            energy += received;
            notifyUpdate();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!isConnector()) return 0;
        int extracted = Math.min(energy, Math.min(MAX_TRANSFER, maxExtract));
        if (!simulate) {
            energy -= extracted;
            notifyUpdate();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return MAX_ENERGY;
    }

    @Override
    public boolean canReceive() {
        return isConnector();
    }

    @Override
    public boolean canExtract() {
        return isConnector();
    }
}
