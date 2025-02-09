package de.mrjulsen.wires.network;

import java.util.UUID;

import org.joml.Vector3f;

import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.util.Utils;
import de.mrjulsen.wires.WireConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class WireConnectionSyncData {

    private static final String NBT_CONNECTION_ID = "Id";
    private static final String NBT_BLOCK_START = "StartBlock";
    private static final String NBT_BLOCK_END = "EndBlock";
    private static final String NBT_START = "Start";
    private static final String NBT_END = "End";
    private static final String NBT_WIRE_TYPE = "WireType";
    private static final String NBT_CONNECTOR_A_DATA = "ConnectorA";
    private static final String NBT_CONNECTOR_B_DATA = "ConnectorB";
    private static final String NBT_CREATION_DATA = "CreationData";
    private static final String NBT_ORIGIN_CHUNK_SECTION = "OriginChunkSection";

    private final UUID connectionId;
    private final Vector3f startPos;
    private final Vector3f endPos;
    private final BlockPos startBlockPos;
    private final BlockPos endBlockPos;
    private final ResourceLocation wireType;
    private final CompoundTag connectorAData;
    private final CompoundTag connectorBData;
    private final CompoundTag creationData;
    private final SectionPos originChunkSection;

    public WireConnectionSyncData(UUID connectionId, BlockPos startBlockPos, BlockPos endBlockPos, Vector3f startPos, Vector3f endPos, ResourceLocation wireType,
            CompoundTag connectorAData, CompoundTag connectorBData, CompoundTag creationData,
            SectionPos originChunkSection) {
        this.connectionId = connectionId;
        this.startBlockPos = startBlockPos;
        this.endBlockPos = endBlockPos;
        this.startPos = startPos;
        this.endPos = endPos;
        this.wireType = wireType;
        this.connectorAData = connectorAData;
        this.connectorBData = connectorBData;
        this.creationData = creationData;
        this.originChunkSection = originChunkSection;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_CONNECTION_ID, connectionId);
        Utils.putNbtBlockPos(nbt, NBT_BLOCK_START, startBlockPos);
        Utils.putNbtBlockPos(nbt, NBT_BLOCK_END, endBlockPos);
        Utils.putNbtVector3f(nbt, NBT_START, startPos);
        Utils.putNbtVector3f(nbt, NBT_END, endPos);
        nbt.putString(NBT_WIRE_TYPE, wireType.toString());
        nbt.put(NBT_CONNECTOR_A_DATA, connectorAData);
        nbt.put(NBT_CONNECTOR_B_DATA, connectorBData);
        nbt.put(NBT_CREATION_DATA, creationData);
        Utils.putNbtSectionPos(nbt, NBT_ORIGIN_CHUNK_SECTION, originChunkSection);
        return nbt;
    }

    public static WireConnectionSyncData fromNbt(CompoundTag nbt) {
        return new WireConnectionSyncData(
            nbt.getUUID(NBT_CONNECTION_ID),
            Utils.getNbtBlockPos(nbt, NBT_BLOCK_START), 
            Utils.getNbtBlockPos(nbt, NBT_BLOCK_END), 
            Utils.getNbtVector3f(nbt, NBT_START), 
            Utils.getNbtVector3f(nbt, NBT_END), 
            Utils.resLoc(nbt.getString(NBT_WIRE_TYPE)), 
            nbt.getCompound(NBT_CONNECTOR_A_DATA), 
            nbt.getCompound(NBT_CONNECTOR_B_DATA),
            nbt.getCompound(NBT_CREATION_DATA),
            Utils.getNbtSectionPos(nbt, NBT_ORIGIN_CHUNK_SECTION)
        );
    }

    public static WireConnectionSyncData of(WireConnection wireConnection) {
        return new WireConnectionSyncData(
            wireConnection.getId(),
            wireConnection.getPointA(),
            wireConnection.getPointB(),
            wireConnection.getRelativeStart(),
            wireConnection.getRelativeEnd(),
            wireConnection.getWireType().getRegistryId(),
            wireConnection.getConnectionANbt(),
            wireConnection.getConnectionBNbt(),
            wireConnection.getCreationDataContext(),
            wireConnection.originChunkSection()
        );
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public Vector3f getStartPos() {
        return startPos;
    }

    public Vector3f getEndPos() {
        return endPos;
    }

    public BlockPos getStartBlockPos() {
        return startBlockPos;
    }

    public BlockPos getEndBlockPos() {
        return endBlockPos;
    }

    public ResourceLocation getWireType() {
        return wireType;
    }

    public CompoundTag getConnectorAData() {
        return connectorAData;
    }

    public CompoundTag getConnectorBData() {
        return connectorBData;
    }

    public CompoundTag getCreationData() {
        return creationData;
    }

    public SectionPos getOriginChunkSection() {
        return originChunkSection;
    }

    public Vector3f getWireAttachPointA() {
        return Utils.getNbtVector3f(getConnectorAData(), IWireConnector.NBT_WIRE_ATTACH_POINT);
    }

    public Vector3f getWireAttachPointB() {
        return Utils.getNbtVector3f(getConnectorBData(), IWireConnector.NBT_WIRE_ATTACH_POINT);
    }
    
}
