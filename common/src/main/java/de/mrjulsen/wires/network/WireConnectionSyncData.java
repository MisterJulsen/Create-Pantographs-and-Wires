package de.mrjulsen.wires.network;

import java.util.Collection;
import java.util.UUID;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import org.joml.Vector3f;

import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.decoration.WireDecorationData;
import de.mrjulsen.wires.util.Utils;
import de.mrjulsen.wires.WireConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
    private static final String NBT_DECORATIONS = "Decorations";
    private static final String NBT_ORIGIN_CHUNK_SECTION = "OriginChunkSection";

    private final UUID connectionId;
    private final Vector3f startPos;
    private final Vector3f endPos;
    private final BlockPos startBlockPos;
    private final BlockPos endBlockPos;
    private final ResourceLocation wireType;
    private final CompoundTag connectorAData;
    private final CompoundTag connectorBData;
    private final CompoundTag customData;
    private final SectionPos originChunkSection;
    private final Collection<WireDecorationData> decorations;

    public WireConnectionSyncData(UUID connectionId, BlockPos startBlockPos, BlockPos endBlockPos, Vector3f startPos, Vector3f endPos, ResourceLocation wireType,
            CompoundTag connectorAData, CompoundTag connectorBData, CompoundTag customData,
            SectionPos originChunkSection, Collection<WireDecorationData> decorations) {
        this.connectionId = connectionId;
        this.startBlockPos = startBlockPos;
        this.endBlockPos = endBlockPos;
        this.startPos = startPos;
        this.endPos = endPos;
        this.wireType = wireType;
        this.connectorAData = connectorAData;
        this.connectorBData = connectorBData;
        this.customData = customData;
        this.originChunkSection = originChunkSection;
        this.decorations = decorations;
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
        nbt.put(NBT_CREATION_DATA, customData);
        Utils.putNbtSectionPos(nbt, NBT_ORIGIN_CHUNK_SECTION, originChunkSection);
        ListTag decoList = new ListTag();
        for (WireDecorationData deco : decorations) {
            decoList.add(deco.toNbt());
        }
        nbt.put(NBT_DECORATIONS, decoList);
        return nbt;
    }

    public static WireConnectionSyncData fromNbt(CompoundTag nbt) {
        return new WireConnectionSyncData(
            nbt.getUUID(NBT_CONNECTION_ID),
            Utils.getNbtBlockPos(nbt, NBT_BLOCK_START), 
            Utils.getNbtBlockPos(nbt, NBT_BLOCK_END), 
            Utils.getNbtVector3f(nbt, NBT_START), 
            Utils.getNbtVector3f(nbt, NBT_END), 
            DLUtils.resourceLocation(nbt.getString(NBT_WIRE_TYPE)),
            nbt.getCompound(NBT_CONNECTOR_A_DATA), 
            nbt.getCompound(NBT_CONNECTOR_B_DATA),
            nbt.getCompound(NBT_CREATION_DATA),
            Utils.getNbtSectionPos(nbt, NBT_ORIGIN_CHUNK_SECTION),
            nbt.getList(NBT_DECORATIONS, Tag.TAG_COMPOUND).stream().map(x -> WireDecorationData.fromNbt((CompoundTag)x)).toList()
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
            wireConnection.getCustomData(),
            wireConnection.originChunkSection(),
            wireConnection.getDecorations()
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

    public CompoundTag getCustomData() {
        return customData;
    }

    public SectionPos getOriginChunkSection() {
        return originChunkSection;
    }

    public Collection<WireDecorationData> getDecorations() {
        return decorations;
    }
    
}
