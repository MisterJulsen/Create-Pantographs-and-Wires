package de.mrjulsen.wires;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.wires.block.IWireConnector;
import de.mrjulsen.wires.network.NetworkManager;
import de.mrjulsen.wires.network.WireChunkLoadingData;
import de.mrjulsen.wires.network.WireConnectionSyncData;
import de.mrjulsen.wires.network.WiresNetworkSyncData;
import de.mrjulsen.wires.network.WiresNetworkSyncData.WireSyncDataEntry;
import de.mrjulsen.wires.WireCollision.WireBlockCollision;
import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

public final class WireNetwork extends SavedData implements IWireNetwork {

    private static final Map<ResourceLocation, WireNetwork> NETWORKS = new HashMap<>();
    private static final int VERSION = 1;

    private static final String NBT_CONNECTIONS = "Connections"; 
    private static final String NBT_VERSION = "NetworkVersion"; 

    private final Level level;

    protected WireNetwork(Level level) {
        this.level = level;
        NETWORKS.put(level.dimensionTypeId().location(), this);
    }

    // Chunks Loading
    private final Multimap<ChunkPos, UUID> playersWatchingChunk = MultimapBuilder.hashKeys().hashSetValues().build();

    // Connections
    private final Map<UUID, WireConnection> connectionsById = new HashMap<>();
    private final Multimap<SectionPos, WireConnection> connectionsBySection = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Multimap<BlockPos, WireConnection> connectionsByBlock = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Multimap<Integer, WireConnection> connectionsByHash = MultimapBuilder.hashKeys().hashSetValues().build();

    // Collision
    private final Multimap<ChunkPos, WireCollision> collisionByChunk = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Multimap<SectionPos, WireCollision> collisionBySection = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Multimap<BlockPos, WireCollision> collisionByBlock = MultimapBuilder.hashKeys().hashSetValues().build();

    public String debug_text() {
        return String.format("Wires[S]: Con: [%s,%s,%s], Col: [%s,%s,%s], P: %s, Id: %s",
            connectionsByBlock.size(),
            connectionsBySection.size(),
            connectionsByHash.size(),

            collisionByChunk.size(),
            collisionBySection.size(),
            collisionByBlock.size(),
            
            playersWatchingChunk.size(),
            connectionsById.size()
        );
    }

    public static void clear() {        
        NETWORKS.clear();
    }
    
    public static WireNetwork get(Level level) {
        return NETWORKS.computeIfAbsent(level.dimensionTypeId().location(), x -> new WireNetwork(level));
    }

    public static WireNetwork create(ServerLevel level) {
        WireNetwork network = new WireNetwork(level);
        loadLegacy(level).ifPresent(x -> {
            applyData(network, x);
            network.setDirty();
        });
        return network;
    }

    public static WireNetwork load(ServerLevel level, CompoundTag nbt) {
        WireNetwork network = new WireNetwork(level);
        applyData(network, nbt);
        return network;
    }

    private static void applyData(WireNetwork network, CompoundTag nbt) {
        nbt.getList(NBT_CONNECTIONS, Tag.TAG_COMPOUND).stream().map(x -> WireConnection.fromNbt((CompoundTag)x)).forEach(x -> {
            if (x.isPresent()) {
                network.setWireConnection(null, x.get());
            }
        });
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag connections = new ListTag();
        for (WireConnection connection : connectionsByHash.values()) {
            connections.add(connection.toNbt());
        }
        nbt.put(NBT_CONNECTIONS, connections);
        nbt.putInt(NBT_VERSION, VERSION);
        return nbt;
    }

    public static String getFileId(ResourceKey<DimensionType> dimension) {
        return WiresApi.MOD_ID + "_wire_network";
    }
    
    @Deprecated
    private static Optional<CompoundTag> loadLegacy(ServerLevel overworld) {
        if (overworld != overworld.getServer().overworld()) return Optional.empty();

        String path = overworld.getServer().getWorldPath(new LevelResource("data/pantographsandwires_wire_network.nbt")).toString();
        File settingsFile = new File(path);

        if (!settingsFile.exists()) {
            return Optional.empty();
        }

        try {
            Optional<CompoundTag> nbt = Optional.ofNullable(NbtIo.readCompressed(settingsFile));
            settingsFile.deleteOnExit();
            return nbt;
        } catch (Exception e) {
            WiresApi.LOGGER.error("Unable to load legacy wire network data.", e);
        }
        return Optional.empty();
    }

    public Level level() {
        return level;
    }

    public Collection<WireConnection> getConnectionsTroughBlock(BlockPos pos) {
        Collection<WireConnection> connections = new LinkedList<>();
        for (WireCollision c : collisionByBlock.get(pos)) {
            connections.add(connectionsById.get(c.getId()));
        }
        return connections;
    }

    public Collection<WireConnection> getConnectionsTroughSection(SectionPos pos) {
        Collection<WireConnection> connections = new LinkedList<>();
        for (WireCollision c : collisionBySection.get(pos)) {
            connections.add(connectionsById.get(c.getId()));
        }
        return connections;
    }

    public Collection<WireConnection> getConnectionsTroughChunk(ChunkPos pos) {
        Collection<WireConnection> connections = new LinkedList<>();
        for (WireCollision c : collisionByChunk.get(pos)) {
            connections.add(connectionsById.get(c.getId()));
        }
        return connections;
    }

    public Collection<WireCollision> getCollisionsTroughBlock(BlockPos pos) {
        return collisionByBlock.get(pos);
    }

    public Collection<WireCollision> getCollisionsTroughSection(SectionPos pos) {
        return collisionBySection.get(pos);
    }

    public Collection<WireCollision> getCollisionsTroughChunk(ChunkPos pos) {
        return collisionByChunk.get(pos);
    }

    public Collection<WireBlockCollision> getCollisionsInBlock(BlockPos pos) {
        Collection<WireBlockCollision> connections = new LinkedList<>();
        for (WireCollision c : collisionByBlock.get(pos)) {
            connections.addAll(c.collisionsInBlock(pos));
        }
        return connections;
    }

    public synchronized boolean addConnection(Level level, CompoundTag itemData, BlockPos posA, BlockPos posB, IWireConnector connectorA, IWireConnector connectorB, IWireType wireType) {
        CompoundTag connectionANbt = connectorA.wireRenderData(level, posA, level.getBlockState(posA), itemData, true);
        CompoundTag connectionBNbt = connectorB.wireRenderData(level, posB, level.getBlockState(posB), itemData, false);

        WireConnection wireConnection = createWireConnection(posA, posB, wireType, connectionANbt, connectionBNbt, itemData);

        if (!wireType.allowMultiConnections() && connectionsByHash.containsKey(wireConnection.hashCode())) {
            return false;
        }
        
        return setWireConnection(level, wireConnection);
    }

    protected synchronized boolean setWireConnection(@Nullable Level level, WireConnection wireConnection) {
        connectionsById.put(wireConnection.getId(), wireConnection);
        connectionsByBlock.put(wireConnection.getPointA(), wireConnection);
        connectionsByBlock.put(wireConnection.getPointB(), wireConnection);
        connectionsBySection.put(SectionPos.of(wireConnection.getPointA()), wireConnection);
        connectionsBySection.put(SectionPos.of(wireConnection.getPointB()), wireConnection);
        connectionsByHash.put(wireConnection.hashCode(), wireConnection);
        
        WireConnectionSyncData syncData = WireConnectionSyncData.of(wireConnection);
        WireCollision collision = new WireCollision(collisionByChunk, collisionBySection, collisionByBlock, wireConnection.getId(), wireConnection.getPointA(), wireConnection.getWireType().buildWire(WireCreationContext.COLLISION, level, syncData).getCollisions());
        wireConnection.setCollisionData(collision);
        wireConnection.setWireConnectionSyncData(syncData);
        
        if (level != null) {
            WiresNetworkSyncData netData = new WiresNetworkSyncData(null, List.of(new WireSyncDataEntry(syncData, true)));

            Set<UUID> updatePlayers = new HashSet<>();
            for (SectionPos section : collision.sectionsIn()) {
                updatePlayers.addAll(playersWatchingChunk.get(section.chunk()));
            }
            
            for (UUID playerId : updatePlayers) {
                if (level.getPlayerByUUID(playerId) instanceof ServerPlayer serverPlayer) {
                    DataAccessor.getFromClient(serverPlayer, netData, NetworkManager.WIRE_CONNECTOR_DATA_TRANSFER, $ -> {});
                }
            }
        }        

        setDirty();
        return true;
    }

    private synchronized WireConnection createWireConnection(BlockPos posA, BlockPos posB, IWireType wireType, CompoundTag connectionANbt, CompoundTag connectionBNbt, CompoundTag itemData) {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (connectionsById.containsKey(id));
        
        WireConnection wireConnection = new WireConnection(id, posA, posB, wireType, connectionANbt, connectionBNbt, itemData);
        return wireConnection;
    }

    public synchronized void removeConnector(Level level, BlockPos pos) {
        if (!connectionsByBlock.containsKey(pos)) {
            return;
        }

        Collection<WireConnection> blockConnections = connectionsByBlock.removeAll(pos);

        Set<UUID> updatePlayers = new HashSet<>();
        for (WireConnection connection : blockConnections) {
            updatePlayers.addAll(removeWireConnection(connection));
        }
        for (UUID playerId : updatePlayers) {
            if (level.getPlayerByUUID(playerId) instanceof ServerPlayer serverPlayer) {
                DataAccessor.getFromClient(serverPlayer, blockConnections.stream().map(x -> x.getId()).toArray(UUID[]::new), NetworkManager.DELETE_WIRE_CONNECTION, $ -> {});
            }
        }

        setDirty();
    }

    private synchronized Set<UUID> removeWireConnection(UUID connection) {
        return removeWireConnection(connectionsById.get(connection));
    }

    private synchronized Set<UUID> removeWireConnection(WireConnection connection) {
        Set<UUID> updatePlayers = new HashSet<>();
        collisionByBlock.values().removeIf(x -> x.getId().equals(connection.getId()));
        collisionByChunk.values().removeIf(x -> x.getId().equals(connection.getId()));
        collisionBySection.values().removeIf(x -> x.getId().equals(connection.getId()));
        connectionsByBlock.values().removeIf(x -> x == connection);
        connectionsBySection.values().removeIf(x -> x == connection);
        connectionsByHash.values().removeIf(x -> x == connection);
        connectionsById.remove(connection.getId());

        for (SectionPos section : connection.getCollisionData().sectionsIn()) {
            ChunkPos chunk = section.chunk();
            if (playersWatchingChunk.containsKey(chunk)) {
                updatePlayers.addAll(playersWatchingChunk.get(chunk));
            }
        }

        setDirty();
        return updatePlayers;
    }
    

    public synchronized void removeBlockedConnection(Level level, BlockPos pos) {
        if (!collisionByBlock.containsKey(pos)) {
            return;
        }

        Collection<WireCollision> collisionsByBlock = collisionByBlock.removeAll(pos);        

        Set<UUID> updatePlayers = new HashSet<>();
        for (WireCollision connection : collisionsByBlock) {
            updatePlayers.addAll(removeWireConnection(connection.getId()));
        }

        for (UUID playerId : updatePlayers) {
            if (level.getPlayerByUUID(playerId) instanceof ServerPlayer serverPlayer) {
                DataAccessor.getFromClient(serverPlayer, collisionsByBlock.stream().toArray(UUID[]::new), NetworkManager.DELETE_WIRE_CONNECTION, $ -> {});
            }
        }

        setDirty();
    }

    /*
     * EVENTS
     */

    public void notifyBlockUpdate(Level level, BlockPos pos, BlockState newState, int flags) {
        if (ModServerConfig.BLOCKS_BREAK_WIRES.get() && !level.isClientSide() && !newState.getCollisionShape(level, pos).isEmpty()) {
            Collection<WireConnection> connections = getConnectionsTroughBlock(pos);
            if (connections.isEmpty()) {
                return;
            }

            Map<WireConnection, BlockPos> connectionsToBreak = new HashMap<>();

            for (WireConnection connection : connections) {
                Collection<WireBlockCollision> collisions = connection.getCollisionData().collisionsInBlock(pos);
                for (WireBlockCollision collision : collisions) {
                    Vector3f vecA = collision.entryPointA();
                    Vector3f vecB = collision.entryPointB();
                    BlockPos dropPos = pos;
                    if (WireCollision.connectionBlocked(level, pos, newState, vecA, vecB)) {
                        for (Direction d : Direction.values()) {
                            if (level.isEmptyBlock(pos.relative(d))) {
                                dropPos = dropPos.relative(d);
                                break;
                            }
                        }								
                        connectionsToBreak.put(connection, dropPos);
                    }
                }
            }

            // TODO Drop wire item

            Set<UUID> updatePlayers = new HashSet<>();
            for (Map.Entry<WireConnection, BlockPos> connection : connectionsToBreak.entrySet()) {
                updatePlayers.addAll(removeWireConnection(connection.getKey()));                
            }
            
            for (UUID playerId : updatePlayers) {
                if (level.getPlayerByUUID(playerId) instanceof ServerPlayer serverPlayer) {
                    DataAccessor.getFromClient(serverPlayer, connectionsToBreak.keySet().stream().map(x -> x.getId()).toArray(UUID[]::new), NetworkManager.DELETE_WIRE_CONNECTION, $ -> {});
                }
            }
		}
    }

    public void checkEntityCollision(Level level, BlockPos pos, Entity entity) {
        /*
		if (ModServerConfig.WIRE_ENTITY_DAMAGE.get() && !level.isClientSide() && entity instanceof LivingEntity living && !(living instanceof Player player && player.getAbilities().invulnerable)) {
            Collection<WireConnection> connections = collisionByBlock.get(pos);
            if (connections.isEmpty()) {
                return;
            }

			for (WireConnection connection : connections) {
                Collection<WireBlockCollision> collisions = connection.getCollisionData().collisionsInBlock(pos);
                for (WireBlockCollision collision : collisions) {
                    Vec3 vecA = collision.entryPointA();
                    Vec3 vecB = collision.entryPointB();
                    double extra = 0;// TODO shockWire.getDamageRadius();
                    AABB hitbox = entity.getBoundingBox();
                    AABB includingExtra = hitbox.inflate(extra).move(-pos.getX(), -pos.getY(), -pos.getZ());
                    if (includingExtra.contains(vecA) || includingExtra.contains(vecB) || includingExtra.clip(vecA, vecB).isPresent()) {
                        entity.hurt(level.damageSources().generic(), 100);
                    }
                }
            }
		}
        */
	}

    public void onChunkLoad(Level level, ChunkPos pos, Player player) {
        playersWatchingChunk.put(pos, player.getUUID());
        synchronized (collisionByChunk) {
            if (collisionByChunk.containsKey(pos) && player instanceof ServerPlayer serverPlayer) {
                Collection<WireConnection> connections = new ArrayList<>(getConnectionsTroughChunk(pos));
                Collection<WireSyncDataEntry> syncData = new ArrayList<>(connections.size());
                for (WireConnection connection : connections) {
                    boolean b = connection.recalcAttachPoints(this, collisionByChunk, collisionBySection, collisionByBlock);
                    syncData.add(new WireSyncDataEntry(connection.getWireConnectionSyncData(), b));
                }
                DataAccessor.getFromClient(serverPlayer, new WiresNetworkSyncData(pos, syncData), NetworkManager.WIRE_CONNECTOR_DATA_TRANSFER, $ -> {});
            }
        }
    }

    public void onChunkUnload(Level level, ChunkPos pos, Player player) {
        if (playersWatchingChunk.containsKey(pos)) {
            playersWatchingChunk.get(pos).removeIf(x -> x.equals(player.getUUID()));
        }
        synchronized (collisionByChunk) {
            if (collisionByChunk.containsKey(pos) && player instanceof ServerPlayer serverPlayer) {
                Collection<WireConnection> connections = getConnectionsTroughChunk(pos);
                if (connections.isEmpty()) return;
                DataAccessor.getFromClient(serverPlayer, new WireChunkLoadingData(pos, connections.stream().map(WireConnection::getId).collect(Collectors.toSet()), false), NetworkManager.WIRE_CONNECTION_CHUNK_LOADING, $ -> {});
            }
        }
    }
}
