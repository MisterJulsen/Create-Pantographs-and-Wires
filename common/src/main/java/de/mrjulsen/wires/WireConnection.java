package de.mrjulsen.wires;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.joml.Vector3f;

import com.google.common.collect.Multimap;

import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.network.WireConnectionSyncData;
import de.mrjulsen.wires.util.Utils;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

public class WireConnection {

    private static final String NBT_ID = "Id";
    private static final String NBT_POS_A = "PosA";
    private static final String NBT_POS_B = "PosB";
    private static final String NBT_WIRE_TYPE = "WireType";
    private static final String NBT_CONNECTION_DATA_A = "CachedDataA";
    private static final String NBT_CONNECTION_DATA_B = "CachedDataB";
    private static final String NBT_CREATION_DATA = "CreationData";

    private final UUID id;
    private final BlockPos pointA;
    private final BlockPos pointB;
    private final IWireType wireType;
    private CompoundTag connectionANbt; // ConnectorA data
    private CompoundTag connectionBNbt; // ConnectorA data
    private final CompoundTag creationData; // = itemData: Additional data from the item when the wire was created.

    // Server
    private WireCollision collisionRef;
    private WireConnectionSyncData syncData;

    private final Cache<Integer> hashCache = new Cache<>(() -> {
        return 31 * Objects.hash(getPointA(), getPointB(), getConnectionANbt(), getConnectionBNbt(), getWireType().getRegistryId()) * Objects.hash(getPointB(), getPointA(), getConnectionBNbt(), getConnectionANbt(), getWireType().getRegistryId());
    });
    
    public WireConnection(UUID id, BlockPos pointA, BlockPos pointB, IWireType type, CompoundTag connectionANbt, CompoundTag connectionBNbt, CompoundTag creationData) {
        this.id = id;
        this.pointA = pointA;
        this.pointB = pointB;
        this.wireType = type;
        this.connectionANbt = connectionANbt;
        this.connectionBNbt = connectionBNbt;
        this.creationData = creationData;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_ID, id);
        DLUtils.putNbtBlockPos(nbt, NBT_POS_A, pointA);
        DLUtils.putNbtBlockPos(nbt, NBT_POS_B, pointB);
        nbt.putString(NBT_WIRE_TYPE, wireType.getRegistryId().toString());
        nbt.put(NBT_CONNECTION_DATA_A, connectionANbt);
        nbt.put(NBT_CONNECTION_DATA_B, connectionBNbt);
        nbt.put(NBT_CREATION_DATA, creationData);
        return nbt;
    }

    public static Optional<WireConnection> fromNbt(CompoundTag nbt) {
        ResourceLocation wireTypeId = Utils.resLoc(nbt.getString(NBT_WIRE_TYPE));
        if (WireTypeRegistry.has(wireTypeId)) {
            return Optional.of(new WireConnection(
                nbt.getUUID(NBT_ID),
                DLUtils.getNbtBlockPos(nbt, NBT_POS_A),
                DLUtils.getNbtBlockPos(nbt, NBT_POS_B),
                WireTypeRegistry.get(wireTypeId),
                nbt.getCompound(NBT_CONNECTION_DATA_A),
                nbt.getCompound(NBT_CONNECTION_DATA_B),
                nbt.getCompound(NBT_CREATION_DATA)
            ));
        }
        return Optional.empty();        
    }

    public boolean recalcAttachPoints(WireNetwork network, Multimap<ChunkPos, WireCollision> chunkMap, Multimap<SectionPos, WireCollision> sectionMap, Multimap<BlockPos, WireCollision> blockMap) {
        boolean hasChanged = false;
        if (network.level().isLoaded(getPointA()) && network.level().getBlockState(getPointA()).getBlock() instanceof IWireConnector c) {
            CompoundTag connectorData = c.wireRenderData(network.level(), getPointA(), network.level().getBlockState(getPointA()), getCreationDataContext(), true);
            if (!connectionANbt.equals(connectorData)) {
                this.connectionANbt = connectorData;
                hasChanged = true;
            }
        }
        if (network.level().isLoaded(getPointB()) && network.level().getBlockState(getPointB()).getBlock() instanceof IWireConnector c) {
            CompoundTag connectorData = c.wireRenderData(network.level(), getPointB(), network.level().getBlockState(getPointB()), getCreationDataContext(), false);
            if (!connectionBNbt.equals(connectorData)) {
                this.connectionBNbt = connectorData;
                hasChanged = true;
            }
        }
        if (!hasChanged) return false;
        WireConnectionSyncData sync = WireConnectionSyncData.of(this);
        WireCollision collision = new WireCollision(chunkMap, sectionMap, blockMap, this.getId(), getPointA(), getWireType().buildWire(WireCreationContext.COLLISION, network.level(), sync).getCollisions());
        setCollisionData(collision);
        setWireConnectionSyncData(sync);
        WiresApi.LOGGER.warn("A wire was misaligned! Data has been corrected. ID: {}, PointA: {}, PointB: {}", id, pointA, pointB);
        return true; 
    }

    public UUID getId() {
        return id;
    }

    public BlockPos getPointA() {
        return pointA;
    }

    public BlockPos getPointB() {
        return pointB;
    }

    public IWireType getWireType() {
        return wireType;
    }    

    public CompoundTag getConnectionANbt() {
        return connectionANbt;
    } 

    public CompoundTag getConnectionBNbt() {
        return connectionBNbt;
    }

    public CompoundTag getCreationDataContext() {
        return creationData;
    }

    public void setCollisionData(WireCollision data) {
        this.collisionRef = data;
    }

    public WireCollision getCollisionData() {
        return collisionRef;
    }

    public void setWireConnectionSyncData(WireConnectionSyncData data) {
        this.syncData = data;
    }

    public WireConnectionSyncData getWireConnectionSyncData() {
        return syncData;
    }


    public SectionPos originChunkSection() {
        return SectionPos.of(pointA);
    }
    
    public Vector3f getRelativeStart() {
        return calcRelative(pointA);
    }

    public Vector3f getRelativeEnd() {  
        return calcRelative(pointB);
    }

    public Vector3f calcRelative(BlockPos pos) {  
        BlockPos sectionPos = originChunkSection().origin();
        return new Vector3f(
            pos.getX() - sectionPos.getX(),
            pos.getY() - sectionPos.getY(),
            pos.getZ() - sectionPos.getZ()
        );
    }

    @Override
    public int hashCode() {
        return hashCache.get();
    }
}
