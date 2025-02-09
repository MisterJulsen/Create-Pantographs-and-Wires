package de.mrjulsen.wires;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.Files;

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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

public final class WireNetwork {

    private WireNetwork() {}

    private static final String FILENAME = WiresApi.MOD_ID + "_wire_network.nbt"; 
    private static final String NBT_CONNECTIONS = "Connections"; 
    
    private static final Multimap<ChunkPos, UUID> playersWatchingChunk = MultimapBuilder.hashKeys().hashSetValues().build();

    // Connections
    private static final Map<UUID, WireConnection> connectionsById = new HashMap<>();
    private static final Multimap<SectionPos, WireConnection> connectionsBySection = MultimapBuilder.hashKeys().hashSetValues().build();
    private static final Multimap<BlockPos, WireConnection> connectionsByBlock = MultimapBuilder.hashKeys().hashSetValues().build();
    private static final Multimap<Integer, WireConnection> connectionsByHash = MultimapBuilder.hashKeys().hashSetValues().build();

    // Collision
    private static final Multimap<ChunkPos, WireCollision> collisionByChunk = MultimapBuilder.hashKeys().hashSetValues().build();
    private static final Multimap<SectionPos, WireCollision> collisionBySection = MultimapBuilder.hashKeys().hashSetValues().build();
    private static final Multimap<BlockPos, WireCollision> collisionByBlock = MultimapBuilder.hashKeys().hashSetValues().build();

    public static void clearConnectionCaches() {
    }

    public static String debug_text() {
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
        playersWatchingChunk.clear();
        connectionsByBlock.clear();
        connectionsBySection.clear();
        connectionsByHash.clear();
        collisionByBlock.clear();
        collisionByChunk.clear();
        collisionBySection.clear();
        connectionsById.clear();
        clearConnectionCaches();
    }
    
    
    public static synchronized void save(MinecraftServer server) {        
        try {
            CompoundTag nbt = new CompoundTag();
            ListTag connections = new ListTag();
            for (WireConnection connection : connectionsByHash.values()) {
                connections.add(connection.toNbt());
            }
            nbt.put(NBT_CONNECTIONS, connections);

            String path = server.getWorldPath(new LevelResource("data/" + FILENAME)).toString();
            File outFile = new File(path);
            File tempFile = new File(path + ".bak");

            if (outFile.exists()) {
                Files.copy(outFile, tempFile);
            }
            NbtIo.writeCompressed(nbt, outFile);
            WiresApi.LOGGER.debug("Saved wire network data.");
            if (tempFile.exists()) {
                tempFile.delete();
            }
        } catch (Exception e) {
            WiresApi.LOGGER.error("Error while saving wire network data.", e);
        } 
    }

    public static void load(MinecraftServer server) {        
        String path = server.getWorldPath(new LevelResource("data/" + FILENAME)).toString();
        File settingsFile = new File(path);
        File backupFile = new File(path + ".bak");

        if (!settingsFile.exists()) {
            return;
        }

        try {
            loadInternal(settingsFile);
        } catch (Exception e) {
            WiresApi.LOGGER.error("Unable to load wire network data.", e);
            if (backupFile.exists()) {
                WiresApi.LOGGER.warn("Wire Network backup file available, trying to load it...");
                try {
                    loadInternal(backupFile);
                } catch (Exception e2) {
                    WiresApi.LOGGER.error("Unable to load backup wire network data.", e2);
                }
            }
        }
    }

    private static void loadInternal(File file) throws Exception{
        CompoundTag nbt = NbtIo.readCompressed(file);
        nbt.getList(NBT_CONNECTIONS, Tag.TAG_COMPOUND).stream().map(x -> WireConnection.fromNbt((CompoundTag)x)).forEach(x -> {
            if (x.isPresent()) {
                setWireConnection(null, x.get());
            }
        });
    }

    public static Collection<WireConnection> getConnectionsTroughBlock(BlockPos pos) {
        Collection<WireConnection> connections = new LinkedList<>();
        for (WireCollision c : collisionByBlock.get(pos)) {
            connections.add(connectionsById.get(c.getId()));
        }
        return connections;
    }

    public static Collection<WireConnection> getConnectionsTroughSection(SectionPos pos) {
        Collection<WireConnection> connections = new LinkedList<>();
        for (WireCollision c : collisionBySection.get(pos)) {
            connections.add(connectionsById.get(c.getId()));
        }
        return connections;
    }

    public static Collection<WireConnection> getConnectionsTroughChunk(ChunkPos pos) {
        Collection<WireConnection> connections = new LinkedList<>();
        for (WireCollision c : collisionByChunk.get(pos)) {
            connections.add(connectionsById.get(c.getId()));
        }
        return connections;
    }

    public static Collection<WireCollision> getCollisionsTroughBlock(BlockPos pos) {
        return collisionByBlock.get(pos);
    }

    public static Collection<WireCollision> getCollisionsTroughSection(SectionPos pos) {
        return collisionBySection.get(pos);
    }

    public static Collection<WireCollision> getCollisionsTroughChunk(ChunkPos pos) {
        return collisionByChunk.get(pos);
    }

    public static Collection<WireBlockCollision> getCollisionsInBlock(BlockPos pos) {
        Collection<WireBlockCollision> connections = new LinkedList<>();
        for (WireCollision c : collisionByBlock.get(pos)) {
            connections.addAll(c.collisionsInBlock(pos));
        }
        return connections;
    }

    public synchronized static boolean addConnection(Level level, CompoundTag itemData, BlockPos posA, BlockPos posB, IWireConnector connectorA, IWireConnector connectorB, IWireType wireType) {
        CompoundTag connectionANbt = connectorA.wireRenderData(level, posA, level.getBlockState(posA), itemData, true);
        CompoundTag connectionBNbt = connectorB.wireRenderData(level, posB, level.getBlockState(posB), itemData, false);

        WireConnection wireConnection = createWireConnection(posA, posB, wireType, connectionANbt, connectionBNbt, itemData);

        if (!wireType.allowMultiConnections() && connectionsByHash.containsKey(wireConnection.hashCode())) {
            return false;
        }
        
        return setWireConnection(level, wireConnection);
    }

    protected synchronized static boolean setWireConnection(@Nullable Level level, WireConnection wireConnection) {
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

        clearConnectionCaches();
        
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
        
        return true;
    }

    private synchronized static WireConnection createWireConnection(BlockPos posA, BlockPos posB, IWireType wireType, CompoundTag connectionANbt, CompoundTag connectionBNbt, CompoundTag itemData) {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (connectionsById.containsKey(id));
        
        WireConnection wireConnection = new WireConnection(id, posA, posB, wireType, connectionANbt, connectionBNbt, itemData);
        return wireConnection;
    }

    public synchronized static void removeConnector(Level level, BlockPos pos) {
        if (!connectionsByBlock.containsKey(pos)) {
            return;
        }

        Collection<WireConnection> blockConnections = connectionsByBlock.removeAll(pos);
        clearConnectionCaches();

        Set<UUID> updatePlayers = new HashSet<>();
        for (WireConnection connection : blockConnections) {
            updatePlayers.addAll(removeWireConnection(connection));
        }
        for (UUID playerId : updatePlayers) {
            if (level.getPlayerByUUID(playerId) instanceof ServerPlayer serverPlayer) {
                DataAccessor.getFromClient(serverPlayer, blockConnections.stream().map(x -> x.getId()).toArray(UUID[]::new), NetworkManager.DELETE_WIRE_CONNECTION, $ -> {});
            }
        }
    }

    private static synchronized Set<UUID> removeWireConnection(UUID connection) {
        return removeWireConnection(connectionsById.get(connection));
    }

    private static synchronized Set<UUID> removeWireConnection(WireConnection connection) {
        Set<UUID> updatePlayers = new HashSet<>();
        collisionByBlock.values().removeIf(x -> x.getId().equals(connection.getId()));
        collisionByChunk.values().removeIf(x -> x.getId().equals(connection.getId()));
        collisionBySection.values().removeIf(x -> x.getId().equals(connection.getId()));
        connectionsByBlock.values().removeIf(x -> x == connection);
        connectionsBySection.values().removeIf(x -> x == connection);
        connectionsByHash.values().removeIf(x -> x == connection);
        connectionsById.remove(connection.getId());
        
        clearConnectionCaches();

        for (SectionPos section : connection.getCollisionData().sectionsIn()) {
            ChunkPos chunk = section.chunk();
            if (playersWatchingChunk.containsKey(chunk)) {
                updatePlayers.addAll(playersWatchingChunk.get(chunk));
            }
        }
        return updatePlayers;
    }
    

    public synchronized static void removeBlockedConnection(Level level, BlockPos pos) {
        if (!collisionByBlock.containsKey(pos)) {
            return;
        }

        Collection<WireCollision> collisionsByBlock = collisionByBlock.removeAll(pos);
        clearConnectionCaches();

        Set<UUID> updatePlayers = new HashSet<>();
        for (WireCollision connection : collisionsByBlock) {
            updatePlayers.addAll(removeWireConnection(connection.getId()));
        }

        for (UUID playerId : updatePlayers) {
            if (level.getPlayerByUUID(playerId) instanceof ServerPlayer serverPlayer) {
                DataAccessor.getFromClient(serverPlayer, collisionsByBlock.stream().toArray(UUID[]::new), NetworkManager.DELETE_WIRE_CONNECTION, $ -> {});
            }
        }
    }

    /*
     * EVENTS
     */

    public static void notifyBlockUpdate(Level level, BlockPos pos, BlockState newState, int flags) {
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

    public static void checkEntityCollision(Level level, BlockPos pos, Entity entity) {
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

    public static void onChunkLoad(Level level, ChunkPos pos, Player player) {
        playersWatchingChunk.put(pos, player.getUUID());
        synchronized (collisionByChunk) {
            if (collisionByChunk.containsKey(pos) && player instanceof ServerPlayer serverPlayer) {
                Collection<WireConnection> connections = new ArrayList<>(getConnectionsTroughChunk(pos));
                Collection<WireSyncDataEntry> syncData = new ArrayList<>(connections.size());
                for (WireConnection connection : connections) {
                    boolean b = connection.recalcAttachPoints(level, collisionByChunk, collisionBySection, collisionByBlock);
                    syncData.add(new WireSyncDataEntry(connection.getWireConnectionSyncData(), b));
                }
                DataAccessor.getFromClient(serverPlayer, new WiresNetworkSyncData(pos, syncData), NetworkManager.WIRE_CONNECTOR_DATA_TRANSFER, $ -> {});
            }
        }
    }

    public static void onChunkUnload(Level level, ChunkPos pos, Player player) {
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
