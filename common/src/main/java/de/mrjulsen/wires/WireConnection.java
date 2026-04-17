package de.mrjulsen.wires;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.google.common.collect.Multimap;

import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.decoration.WireDecorationData;
import de.mrjulsen.wires.decoration.IWireDecoration;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.network.WireConnectionSyncData;
import de.mrjulsen.wires.util.Utils;
import de.mrjulsen.mcdragonlib.util.Cache;
import de.mrjulsen.mcdragonlib.util.NbtUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class WireConnection {

    private static final String NBT_ID = "Id";
    private static final String NBT_POS_A = "PosA";
    private static final String NBT_POS_B = "PosB";
    private static final String NBT_WIRE_TYPE = "WireType";
    private static final String NBT_CONNECTION_DATA_A = "CachedDataA";
    private static final String NBT_CONNECTION_DATA_B = "CachedDataB";
    private static final String NBT_CREATION_DATA = "CreationData";
    private static final String NBT_DECORATIONS = "Decorations";

    private final UUID id;
    private final BlockPos pointA;
    private final BlockPos pointB;
    private final IWireType wireType;
    private CompoundTag connectionANbt; // ConnectorA data
    private CompoundTag connectionBNbt; // ConnectorA data
    private final CompoundTag customData; // = itemData: Additional data from the item when the wire was created.

    private final Map<String, TreeMap<Double, WireDecorationData>> decorations = new HashMap<>();

    // Server
    private WireCollision collisionRef;
    private WireConnectionSyncData syncData;

    private final Cache<Integer> hashCache = new Cache<>(() -> {
        return 31 * Objects.hash(getPointA(), getPointB(), getConnectionANbt(), getConnectionBNbt(), getWireType().getRegistryId()) * Objects.hash(getPointB(), getPointA(), getConnectionBNbt(), getConnectionANbt(), getWireType().getRegistryId());
    });
    
    public WireConnection(UUID id, BlockPos pointA, BlockPos pointB, IWireType type, CompoundTag connectionANbt, CompoundTag connectionBNbt, CompoundTag customData) {
        this.id = id;
        this.pointA = pointA;
        this.pointB = pointB;
        this.wireType = type;
        this.connectionANbt = connectionANbt;
        this.connectionBNbt = connectionBNbt;
        this.customData = customData;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID(NBT_ID, id);
        NbtUtils.putNbtPos(nbt, NBT_POS_A, pointA);
        NbtUtils.putNbtPos(nbt, NBT_POS_B, pointB);
        nbt.putString(NBT_WIRE_TYPE, wireType.getRegistryId().toString());
        nbt.put(NBT_CONNECTION_DATA_A, connectionANbt);
        nbt.put(NBT_CONNECTION_DATA_B, connectionBNbt);
        nbt.put(NBT_CREATION_DATA, customData);

        ListTag decorationsList = new ListTag();
        for (WireDecorationData deco : getDecorations()) {
            decorationsList.add(deco.toNbt());
        }
        nbt.put(NBT_DECORATIONS, decorationsList);
        return nbt;
    }

    public static Optional<WireConnection> fromNbt(CompoundTag nbt) {
        ResourceLocation wireTypeId = DLUtils.resourceLocation(nbt.getString(NBT_WIRE_TYPE));
        if (WireTypeRegistry.has(wireTypeId)) {
            WireConnection connection = new WireConnection(
                nbt.getUUID(NBT_ID),
                NbtUtils.getNbtBlockPos(nbt, NBT_POS_A),
                NbtUtils.getNbtBlockPos(nbt, NBT_POS_B),
                WireTypeRegistry.get(wireTypeId),
                nbt.getCompound(NBT_CONNECTION_DATA_A),
                nbt.getCompound(NBT_CONNECTION_DATA_B),
                nbt.getCompound(NBT_CREATION_DATA)
            );
            nbt.getList(NBT_DECORATIONS, Tag.TAG_COMPOUND).forEach(x -> {
                connection.addDecoration(WireDecorationData.fromNbt((CompoundTag)x));
            });
            return Optional.of(connection);
        }
        return Optional.empty();        
    }
    

    public boolean addDecoration(Vector3d pos, String wireName, IWireDecoration<?> element) {
        double d = collisionRef.worldPosToWirePos(wireName, pos);
        if (decorations.containsKey(wireName)) {
            TreeMap<Double, WireDecorationData> map = decorations.get(wireName);
            Map.Entry<Double, WireDecorationData> lower = map.lowerEntry(d);
            Map.Entry<Double, WireDecorationData> upper = map.ceilingEntry(d);
            if ((lower != null && lower.getKey() + lower.getValue().getDecoration().getRadius(null) > d - element.getRadius(null)) ||
                (upper != null && upper.getKey() - upper.getValue().getDecoration().getRadius(null) < d + element.getRadius(null))) {
                    return false;
            }
        }
        WireDecorationData decoration = new WireDecorationData(wireName, d, element);
        addDecoration(decoration);
        return true;
    }

    private void addDecoration(WireDecorationData decoration) {
        this.decorations.computeIfAbsent(decoration.getWireName(), x -> new TreeMap<>()).put(decoration.getPos(), decoration);
    }

    public List<WireDecorationData> getDecorationsAt(Vector3d pos, String wireName) {
        double d = collisionRef.worldPosToWirePos(wireName, pos);
        if (!decorations.containsKey(wireName)) {
            return List.of();
        }
        TreeMap<Double, WireDecorationData> map = decorations.get(wireName);
        List<WireDecorationData> decoResult = new ArrayList<>(2);
        for (WireDecorationData decoration : map.values()) {
            if (decoration.getPos() + decoration.getDecoration().getRadius(null) >= d && decoration.getPos() - decoration.getDecoration().getRadius(null) <= d) {
                decoResult.add(decoration);
            }
        }
        return decoResult;
    }

    public void removeDecorations(Level level, Optional<Player> player, String wireName, List<WireDecorationData> decorations) {
        if (this.decorations.containsKey(wireName)) {
            TreeMap<Double, WireDecorationData> map = this.decorations.get(wireName);
            map.values().removeAll(decorations);
            for (WireDecorationData deco : decorations) {
                deco.getDecoration().onBreak(level, collisionRef.wirePosToWorldPos(wireName, deco.getPos()), player);
            }
            if (map.isEmpty()) {
                this.decorations.remove(wireName);
            }
        }
    }

    public boolean recalcAttachPoints(WireGraph network, Multimap<ChunkPos, WireCollision> chunkMap, Multimap<SectionPos, WireCollision> sectionMap, Multimap<BlockPos, WireCollision> blockMap) {
        boolean hasChanged = false;
        if (network.getLevel().isLoaded(getPointA()) && network.getLevel().getBlockState(getPointA()).getBlock() instanceof IWireConnector c) {
            CompoundTag connectorData = null;// c.wireRenderData(network.level(), getPointA(), network.level().getBlockState(getPointA()), getCustomData(), 0);
            if (!connectionANbt.equals(connectorData)) {
                this.connectionANbt = connectorData;
                hasChanged = true;
            }
        }
        if (network.getLevel().isLoaded(getPointB()) && network.getLevel().getBlockState(getPointB()).getBlock() instanceof IWireConnector c) {
            CompoundTag connectorData = null;//c.wireRenderData(network.level(), getPointB(), network.level().getBlockState(getPointB()), getCustomData(), 1);
            if (!connectionBNbt.equals(connectorData)) {
                this.connectionBNbt = connectorData;
                hasChanged = true;
            }
        }
        if (!hasChanged) return false;
        WireConnectionSyncData sync = WireConnectionSyncData.of(this);
        WireCollision collision = null;// new WireCollision(chunkMap, sectionMap, blockMap, this.getId(), getPointA(), getWireType().buildWire(WireCreationContext.COLLISION, network.level(), sync).getCollisions());
        setCollisionData(collision);
        setWireConnectionSyncData(sync);
        WiresApi.LOGGER.warn("A wire was misaligned! Data has been corrected. ID: {}, PointA: {}, PointB: {}", id, pointA, pointB);
        return true; 
    }

    public Collection<WireDecorationData> getDecorations() {
        Collection<WireDecorationData> decorations = new ArrayList<>();
        for (TreeMap<Double, WireDecorationData> decor : this.decorations.values()) {
            for (WireDecorationData d : decor.values()) {
                decorations.add(d);
            }
        }
        return decorations;
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

    public CompoundTag getCustomData() {
        return customData;
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

    public void onRemove(Level level, Vector3f breakPosition, Optional<Player> player) {
        for (TreeMap<Double, WireDecorationData> e : decorations.values()) {
            for (WireDecorationData decoration : e.values()) {
                decoration.getDecoration().onBreak(level, collisionRef.wirePosToWorldPos(decoration.getWireName(), decoration.getPos()), player);
            }
        }
    }
}
