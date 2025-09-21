package de.mrjulsen.wires.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3f;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.graph.NewWireCollision.WireBlockCollision;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.WireBaseItem.CustomData;
import de.mrjulsen.wires.network.DeleteWireSyncData;
import de.mrjulsen.wires.network.NetworkManager;
import de.mrjulsen.wires.network.WireChunkLoadingData;
import de.mrjulsen.wires.network.WiresSyncData;
import de.mrjulsen.wires.util.GraphId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

public class WireGraph extends SavedData implements IWireGraph {

    public static final int VERSION = 3;
    private static final String NBT_VERSION = "Version";
    private static final String NBT_NODES = "Nodes";
    private static final String NBT_EDGES = "Edges";

    private final GraphId id;
    private final Level level;
    private final Map<UUID, WireNode> nodes = new HashMap<>();
    private final Map<UUID, WireEdge> edges = new HashMap<>();
    
    // --- Access ---
    final Multimap<BlockPos, UUID> nodesByBlock = MultimapBuilder.hashKeys().arrayListValues().build();
    final Multimap<SectionPos, UUID> nodesBySection = MultimapBuilder.hashKeys().arrayListValues().build();
    final Multimap<ChunkPos, UUID> nodesByChunk = MultimapBuilder.hashKeys().arrayListValues().build();
    final Map<ResourceLocation, NodeAccessor<?>> nodesByType = new ConcurrentHashMap<>();

    final Multimap<WireNode, WireEdge> edgesByNode = MultimapBuilder.hashKeys().hashSetValues().build();
    
    // Collision
    final Map<UUID, NewWireCollision> collisionById = new HashMap<>();
    final Multimap<BlockPos, NewWireCollision> collisionByBlock = MultimapBuilder.hashKeys().hashSetValues().build();
    final Multimap<ChunkPos, NewWireCollision> collisionByChunk = MultimapBuilder.hashKeys().hashSetValues().build();
    final Multimap<SectionPos, NewWireCollision> collisionBySection = MultimapBuilder.hashKeys().hashSetValues().build();
    
    // Chunk Loading
    private final Multimap<ChunkPos, UUID> playersWatchingChunk = MultimapBuilder.hashKeys().hashSetValues().build();

    public WireGraph(GraphId id, Level level) {
        this.id = id;
        this.level = level;
    }

    @Override
    public DLStatistics getStatistics() {
        DLStatistics.Group nodesGroup = new DLStatistics.Group("nodes", "N");
        DLStatistics.Group edgesGroup = new DLStatistics.Group("edges", "E");
        DLStatistics.Group collisionsGroup = new DLStatistics.Group("collisions", "C");
        DLStatistics.Group playersGroup = new DLStatistics.Group("players", "P");

        return new DLStatistics("Wires[S]", List.of(
            new DLStatistics.Stat(nodesGroup, "Nodes", nodes.size()),
            new DLStatistics.Stat(nodesGroup, "Nodes (by block)", nodesByBlock.size()),
            new DLStatistics.Stat(nodesGroup, "Nodes (by section)", nodesBySection.size()),
            new DLStatistics.Stat(nodesGroup, "Nodes (by chunk)", nodesByChunk.size()),

            new DLStatistics.Stat(edgesGroup, "Edges", edges.size()),
            new DLStatistics.Stat(edgesGroup, "Edges (by node)", edgesByNode.size()),

            new DLStatistics.Stat(collisionsGroup, "Collision", collisionById.size()),
            new DLStatistics.Stat(collisionsGroup, "Collision (by block)", collisionByBlock.size()),
            new DLStatistics.Stat(collisionsGroup, "Collision (by section)", collisionBySection.size()),
            new DLStatistics.Stat(collisionsGroup, "Collision (by chunk)", collisionByChunk.size()),

            new DLStatistics.Stat(playersGroup, "Players Watching Chunks", playersWatchingChunk.size())
        ));
    }


    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag nodes = new ListTag();
        for (WireNode node : this.nodes.values()) {
            nodes.add(node.toNbt());
        }
        ListTag edges = new ListTag();
        for (WireEdge edge : this.edges.values()) {
            edges.add(edge.toNbt());
        }
        nbt.put(NBT_NODES, nodes);
        nbt.put(NBT_EDGES, edges);
        nbt.putInt(NBT_VERSION, VERSION);
        return nbt;
    }    

    public WireGraph load(ServerLevel level, CompoundTag nbt) {
        applyData(nbt);
        return this;
    }

    private void applyData(CompoundTag nbt) {
        Collection<WireNode> nodes = nbt.getList(NBT_NODES, Tag.TAG_COMPOUND).stream().map(x -> WireNode.fromNbt(this, (CompoundTag)x)).toList();
        Collection<WireEdge> edges = nbt.getList(NBT_EDGES, Tag.TAG_COMPOUND).stream().map(x -> WireEdge.fromNbt(this, (CompoundTag)x).get()).toList();
        for (WireNode node : nodes) {
            setNode(node);
        }
        for (WireEdge edge : edges) {
            setEdge(edge);
        }
        setDirty();
    }

    public String getFileId() {
        return getId().id() + "_wire_graph";
    }

    @Override
    public GraphId getId() {
        return id;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public <A extends NodeAccessor<?>> Optional<A> accessNodesOfType(ResourceLocation typeId) {
        return Optional.ofNullable(nodesByType.containsKey(typeId) ? (A)nodesByType.get(typeId) : null);
    }

    public UUID createNewNodeId() {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (nodes.containsKey(id));
        return id;
    }

    public UUID createNewEdgeId() {
        UUID id;
        do {
            id = UUID.randomUUID();
        } while (edges.containsKey(id));
        return id;
    }

    @Override
    public WireNode getNode(UUID id) {
        return nodes.get(id);
    }

    @Override
    public WireEdge getEdge(UUID id) {
        return edges.get(id);
    }

    @Override
    public Collection<WireNode> getNodes() {
        return Collections.synchronizedCollection(Collections.unmodifiableCollection(nodes.values()));
    }

    @Override
    public Collection<WireEdge> getEdges() {
        return Collections.synchronizedCollection(Collections.unmodifiableCollection(edges.values()));
    }

    @Override
    public boolean hasEdge(UUID id) {
        return edges.containsKey(id);
    }

    @Override
    public boolean hasNode(UUID id) {
        return nodes.containsKey(id);
    }


    

    /*
     * MODIFICATION
     */

     /**
      * Creates a new node and adds it to the graph.
      * @param data The node data.
      * @param pos The world position of the node.
      * @return The newly created node.
      */
    public synchronized WireNode createNode(NodeData data, Vector3f pos) {
        WireNode node = new WireNode(this, data);
        node.setPos(pos);
        setNode(node);
        return node;
    }

    private synchronized void setNode(WireNode node) {        
        BlockPos blockPos = new BlockPos((int)node.getPos().x(), (int)node.getPos().y(), (int)node.getPos().z());
        SectionPos section = SectionPos.of(blockPos);
        ChunkPos chunk = new ChunkPos(blockPos);
        this.nodes.put(node.getId(), node);
        this.nodesByBlock.put(blockPos, node.getId());
        this.nodesBySection.put(section, node.getId());
        this.nodesByChunk.put(chunk, node.getId());
        this.nodesByType.computeIfAbsent(node.getData().getRegistryType().id(), (a) -> node.getData().getRegistryType().createAccessor()).put(node);
        setDirty();
    }

    public synchronized void removeNode(UUID id, Vector3f breakPos, Optional<Player> player) {
        if (!nodes.containsKey(id)) {
            return;
        }

        WireNode node = nodes.get(id);
        Collection<WireEdge> edgesToRemove = new ArrayList<>(edgesByNode.get(node));
        for (WireEdge edge : edgesToRemove) {
            removeEdge(edge.getId(), breakPos, player);
        }
        node.onRemove(getLevel(), breakPos, player);

        this.nodes.remove(id);
        this.nodesByBlock.values().removeIf(x -> x.equals(id));
        this.nodesByChunk.values().removeIf(x -> x.equals(id));
        this.nodesBySection.values().removeIf(x -> x.equals(id));
        this.nodesByType.get(node.getData().getRegistryType().id()).remove(node);
        setDirty();
    }



    /**
     * Creates a new edge but doesn't add it to the graph. For this, use {@link WireGraph#setEdge}.
     */
    public synchronized WireEdge createEdge(IWireType type, CustomData customData, NodeData nodeDataA, NodeData nodeDataB, MutableInt pointStartIndex) {
        WireNode nodeA = nodeDataA.getOrCreateNodeInternal(this, type, customData);
        WireNode nodeB = nodeDataB.getOrCreateNodeInternal(this, type, customData);
        WireConnectionData custom = new WireConnectionData(
            customData,
            nodeDataA.getConnectorCustomData(this, customData, nodeA, pointStartIndex.getAndIncrement()).orElse(new ConnectorDataProvider.Empty()),
            nodeDataB.getConnectorCustomData(this, customData, nodeB, pointStartIndex.getAndIncrement()).orElse(new ConnectorDataProvider.Empty())
        );

        if (nodeA == null || nodeB == null) {
            return null;
        }

        WireEdge edge = new WireEdge(this, type, custom, nodeA.getId(), nodeB.getId());
        setEdge(edge);
        return edge;
    }

    public synchronized void setEdge(WireEdge edge) {
        WireBatch batch = edge.getType().buildWire(WireCreationContext.COLLISION, getLevel(), edge.getWireConnectionData(), edge, getNode(edge.getNodeAId()), getNode(edge.getNodeBId()));
        if (batch == null) {
            return;
        }
        
        removeEdgeInternal(edge.getId());

        WireNode nodeA = getNode(edge.getNodeAId());
        WireNode nodeB = getNode(edge.getNodeBId());
        this.edges.put(edge.getId(), edge);
        this.edgesByNode.put(nodeA, edge);
        this.edgesByNode.put(nodeB, edge);
        nodeA.addConnection(edge.getId());
        nodeB.addConnection(edge.getId());        

        NewWireCollision collision = new NewWireCollision(this, edge.getId(), batch.getCollisions());
        collisionById.put(edge.getId(), collision);
        for (BlockPos pos : collision.blocksIn()) {
            collisionByBlock.put(pos, collision);
        }
        for (SectionPos pos : collision.sectionsIn()) {
            collisionBySection.put(pos, collision);
        }
        for (ChunkPos pos : collision.chunksIn()) {
            collisionByChunk.put(pos, collision);
        }
        
        // Sync to clients
        WiresSyncData netData = new WiresSyncData(getId(), null, List.of(edge), List.of(nodeA, nodeB), true);
        for (ServerPlayer player : getPlayersForEdge(edge.getId())) {
            DataAccessor.getFromClient(player, new WiresSyncData.Wrapper(netData), NetworkManager.WIRE_CONNECTOR_DATA_TRANSFER, $ -> {});
        }

        setDirty();
    }
    
    public void sendEdgeToClient(WireEdge edge) {        
        WireNode nodeA = getNode(edge.getNodeAId());
        WireNode nodeB = getNode(edge.getNodeBId());
        WiresSyncData netData = new WiresSyncData(getId(), null, List.of(edge), List.of(nodeA, nodeB), true);
        for (ServerPlayer player : getPlayersForEdge(edge.getId())) {
            DataAccessor.getFromClient(player, new WiresSyncData.Wrapper(netData), NetworkManager.WIRE_CONNECTOR_DATA_TRANSFER, $ -> {});
        }
    }

    public synchronized void removeEdge(UUID id, Vector3f removePosition, Optional<Player> player) {
        if (!edges.containsKey(id)) {
            return;
        }

        // Sync to clients
        for (ServerPlayer serverPlayer : getPlayersForEdge(id)) {
            DataAccessor.getFromClient(serverPlayer, new DeleteWireSyncData(getId(), List.of(id)), NetworkManager.DELETE_WIRE_CONNECTION, $ -> {});
        }

        this.edges.get(id).onRemove(level, removePosition, player);
        WireEdge edge = removeEdgeInternal(id);
        
        if (!getNode(edge.getNodeAId()).removeConnection(id)) {
            removeNode(edge.getNodeAId(), new Vector3f(), Optional.empty());
        }
        if (!getNode(edge.getNodeBId()).removeConnection(id)) {
            removeNode(edge.getNodeBId(), new Vector3f(), Optional.empty());
        }

        setDirty();
    }

    protected synchronized WireEdge removeEdgeInternal(UUID id) {
        WireEdge edge = edges.remove(id);
        edgesByNode.values().removeIf(x -> x.equals(edge));
        NewWireCollision collision = collisionById.remove(id);
        collisionByChunk.values().removeIf(x -> x == collision);
        collisionBySection.values().removeIf(x -> x == collision);
        collisionByBlock.values().removeIf(x -> x == collision);
        return edge;
    }



    /*
    * UTILITIES
    */

    /**
     * Get a list of players who have this edge loaded (according to the chunk watching tracker)
     * @param edgeId The id of the edge
     * @return A list of players
     */
    public Collection<ServerPlayer> getPlayersForEdge(UUID edgeId) {   
        if (!collisionById.containsKey(edgeId))      {
            return List.of();
        }

        Collection<ServerPlayer> players = new ArrayList<>();
        Set<UUID> updatePlayers = new HashSet<>();
        for (SectionPos section : collisionById.get(edgeId).sectionsIn()) {
            updatePlayers.addAll(playersWatchingChunk.get(section.chunk()));
        }        
        for (UUID playerId : updatePlayers) {
            if (level.getPlayerByUUID(playerId) instanceof ServerPlayer serverPlayer) {
                players.add(serverPlayer);
            }
        }
        return players;
    }    
    
    /**
     * Updates the data for this node, which may be changed, for example, when the connection block changes.
     * @param node The node whose data should be updated.
     */
    public void updateNodeData(WireNode node) {
        node.getData().updateWireNode(this, node);
        Iterator<UUID> ids = node.getConnections().iterator();
        while (ids.hasNext()) {
            UUID id = ids.next();
            if (!hasEdge(id)) {
                ids.remove();
                continue;
            }
            
            WireEdge edge = getEdge(id);
            WireNode nodeA = getNode(edge.getNodeAId());
            WireNode nodeB = getNode(edge.getNodeBId());
            CustomData customData = edge.getWireConnectionData().customData();
            edge.setWireConnectionData(new WireConnectionData(
                customData,
                nodeA.getData().getConnectorCustomData(this, customData, nodeA, 0).orElse(edge.getWireConnectionData().connectorA()),
                nodeB.getData().getConnectorCustomData(this, customData, nodeB, 1).orElse(edge.getWireConnectionData().connectorB())
            ));
        }
        //WiresApi.LOGGER.warn("A wire was misaligned! Data has been corrected. ID: {}, PointA: {}, PointB: {}", id, pointA, pointB);
    }
    
    public Collection<NewWireCollision> getCollisionsInChunk(ChunkPos chunk) {
        return Collections.unmodifiableCollection(collisionByChunk.get(chunk));
    }

    public Collection<NewWireCollision> getCollisionsInBlock(BlockPos block) {
        return Collections.unmodifiableCollection(collisionByBlock.get(block));
    }

    public Optional<NewWireCollision> getCollisionById(UUID id) {
        return Optional.ofNullable(collisionById.get(id));
    }



    /*
     * EVENTS
     */

    public void notifyBlockUpdate(Level level, Optional<Player> player, BlockPos pos, BlockState newState, int flags) {
        if (ModServerConfig.BLOCKS_BREAK_WIRES.get() && !level.isClientSide() && !newState.getCollisionShape(level, pos).isEmpty()) {
            Collection<UUID> connections = getCollisionsInBlock(pos).stream().map(x -> x.getId()).toList(); // TODO
            if (connections.isEmpty()) {
                return;
            }

            List<UUID> connectionsToBreak = new ArrayList<>();

            for (UUID connection : connections) {
                Collection<WireBlockCollision> collisions = collisionById.get(connection).collisionsInBlock(pos);
                for (WireBlockCollision collision : collisions) {
                    Vector3f vecA = collision.getInVector();
                    Vector3f vecB = collision.getOutVector();
                    BlockPos dropPos = pos;
                    if (NewWireCollision.isConnectionBlocked(level, pos, newState, vecA, vecB)) {
                        for (Direction d : Direction.values()) {
                            if (level.isEmptyBlock(pos.relative(d))) {
                                dropPos = dropPos.relative(d);
                                break;
                            }
                        }
                        if (!connectionsToBreak.contains(connection)) {
                            connectionsToBreak.add(connection);
                        }
                    }
                }
            }

            // TODO Drop wire item

            for (UUID connection : connectionsToBreak) {
                removeEdge(connection, new Vector3f(pos.getX(), pos.getY(), pos.getZ()), player);
            }

            ChunkPos chunk = new ChunkPos(pos);            
            Set<UUID> updatePlayers = new HashSet<>();
            if (playersWatchingChunk.containsKey(chunk)) {
                updatePlayers.addAll(playersWatchingChunk.get(chunk));
            }
            
            for (UUID playerId : updatePlayers) {
                if (level.getPlayerByUUID(playerId) instanceof ServerPlayer serverPlayer) {
                    DataAccessor.getFromClient(serverPlayer, new DeleteWireSyncData(getId(), connectionsToBreak), NetworkManager.DELETE_WIRE_CONNECTION, $ -> {});
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
        
        synchronized (nodesByChunk) {
            if (nodesByChunk.containsKey(pos) && player instanceof ServerPlayer serverPlayer) {
                Collection<UUID> edgeIds = nodesByChunk.get(pos).stream().flatMap(x -> {
                    WireNode node = getNode(x);
                    updateNodeData(node);
                    return node.getConnections().stream();
                }).toList();
                if (edgeIds.isEmpty()) return;

                Collection<WireEdge> edges = new ArrayList<>(edgeIds.size());
                Set<WireNode> nodes = new HashSet<>();
                for (UUID id : edgeIds) {
                    WireEdge edge = getEdge(id);
                    edges.add(edge);
                    nodes.add(getNode(edge.getNodeAId()));
                    nodes.add(getNode(edge.getNodeBId()));
                }
                DataAccessor.getFromClient(serverPlayer, new WiresSyncData.Wrapper(new WiresSyncData(getId(), pos, edges, nodes, true)), NetworkManager.WIRE_CONNECTOR_DATA_TRANSFER, $ -> {});
            }
        }
    }

    public void onChunkUnload(Level level, ChunkPos pos, Player player) {
        if (playersWatchingChunk.containsKey(pos)) {
            playersWatchingChunk.get(pos).removeIf(x -> x.equals(player.getUUID()));
        }
        
        synchronized (collisionByChunk) {
            if (collisionByChunk.containsKey(pos) && player instanceof ServerPlayer serverPlayer) {
                Collection<UUID> edgeIds = collisionByChunk.get(pos).stream().map(x -> x.getId()).toList();
                if (edgeIds.isEmpty()) return;

                DataAccessor.getFromClient(serverPlayer, new WireChunkLoadingData(getId(), pos, edgeIds, false), NetworkManager.WIRE_CONNECTION_CHUNK_LOADING, $ -> {});
            }
        }
    }

    /*
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
        */
}
