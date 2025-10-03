package de.mrjulsen.wires.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3f;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.mrjulsen.mcdragonlib.util.accessor.DataAccessor;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.config.ModCommonConfig;
import de.mrjulsen.paw.config.ModServerConfig;
import de.mrjulsen.paw.registry.ModWireRegistry;
import de.mrjulsen.wires.IWireType;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.graph.NewWireCollision.WireBlockCollision;
import de.mrjulsen.wires.graph.data.WireEdgeHash;
import de.mrjulsen.wires.graph.data.WireConnectionData;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.graph.data.node.BlockConnectorNodeData;
import de.mrjulsen.wires.graph.data.node.NodeData;
import de.mrjulsen.wires.graph.data.provider.ConnectorDataProvider;
import de.mrjulsen.wires.item.CustomData;
import de.mrjulsen.wires.network.DeleteWireSyncData;
import de.mrjulsen.wires.network.NetworkManager;
import de.mrjulsen.wires.network.WireChunkLoadingData;
import de.mrjulsen.wires.network.WiresSyncData;
import de.mrjulsen.wires.util.GraphId;
import de.mrjulsen.wires.util.Utils;
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
    final Map<WireEdgeHash, WireEdge> edgesByHash = new ConcurrentHashMap<>();
    
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
            new DLStatistics.Stat(edgesGroup, "Edges (by hash)", edgesByHash.size()),

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
        for (Tag tag : nbt.getList(NBT_NODES, Tag.TAG_COMPOUND)) {
            WireNode.fromNbt(this, (CompoundTag)tag).ifPresent(this::setNode);
        }
        for (Tag tag : nbt.getList(NBT_EDGES, Tag.TAG_COMPOUND)) {
            WireEdge.fromNbt(this, (CompoundTag)tag).ifPresent(e -> setEdge(e, true));
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

    public WireEdge getEdge(WireEdgeHash hash) {
        return edgesByHash.get(hash);
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

    public boolean hasEdge(WireEdgeHash hash) {
        return edgesByHash.containsKey(hash);
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

    /**
     * 
     * @param id
     * @param breakPos The break position or {@code null} to disable drops
     * @param player
     */
    public synchronized void removeNode(UUID id, Vector3f breakPos, Optional<Player> player) {
        if (!nodes.containsKey(id)) {
            return;
        }

        WireNode node = nodes.get(id);
        
        if (breakPos != null && player != null) {
            Collection<WireEdge> edgesToRemove = new ArrayList<>(edgesByNode.get(node));
            for (WireEdge edge : edgesToRemove) {
                removeEdge(edge.getId(), breakPos, player);
            }
            node.onRemove(getLevel(), breakPos, player);            
        }

        this.nodes.remove(id);
        this.nodesByBlock.values().removeIf(x -> x.equals(id));
        this.nodesByChunk.values().removeIf(x -> x.equals(id));
        this.nodesBySection.values().removeIf(x -> x.equals(id));
        this.nodesByType.get(node.getData().getRegistryType().id()).remove(node);
        setDirty();
    }


    //private record EdgeKey(IWireType type, CustomData data, NodeData nodeDataA, nodeDataB) {}

    public record CreateEdgeResult(boolean success, int code, Optional<WireEdge> edge) {
        public static final int CONNECTION_EXISTS = 0;
        public static final int INVALID_CONNECTOR = 1;
    }

    /**
     * Creates a new edge but doesn't add it to the graph. For this, use {@link WireGraph#setEdge}.
     */
    public synchronized CreateEdgeResult createEdge(IWireType type, CustomData customData, NodeData nodeDataA, NodeData nodeDataB, MutableInt pointStartIndex, boolean sendToPlayers) {
        WireEdgeHash hash = new WireEdgeHash(customData, nodeDataA, nodeDataB);
        if (edgesByHash.containsKey(hash)) {
            return new CreateEdgeResult(false, CreateEdgeResult.CONNECTION_EXISTS, Optional.empty());
        }

        WireNode nodeA = nodeDataA.getOrCreateNodeInternal(this, type, customData);
        WireNode nodeB = nodeDataB.getOrCreateNodeInternal(this, type, customData);
        if (nodeA == null || nodeB == null) {
            return new CreateEdgeResult(false, CreateEdgeResult.INVALID_CONNECTOR, Optional.empty());
        }

        Optional<ConnectorDataProvider> connectorDataA = nodeDataA.getConnectorCustomData(this, customData, nodeA, pointStartIndex.getAndIncrement());
        Optional<ConnectorDataProvider> connectorDataB = nodeDataB.getConnectorCustomData(this, customData, nodeB, pointStartIndex.getAndIncrement());
        if (connectorDataA.isEmpty() || connectorDataB.isEmpty()) {
            return new CreateEdgeResult(false, CreateEdgeResult.INVALID_CONNECTOR, Optional.empty());
        } 

        WireConnectionData custom = new WireConnectionData(customData, connectorDataA.get(), connectorDataB.get());

        WireEdge edge = new WireEdge(this, type, custom, nodeA.getId(), nodeB.getId(), hash);
        setEdge(edge, sendToPlayers);
            return new CreateEdgeResult(true, -1, Optional.of(edge));
    }

    public synchronized void setEdge(WireEdge edge, boolean updateClients) {
        WireBatch batch = edge.getType().buildWire(WireCreationContext.COLLISION, getLevel(), edge.getWireConnectionData(), edge, getNode(edge.getNodeAId()), getNode(edge.getNodeBId()));
        if (batch == null) {
            return;
        }
        
        removeEdgeInternal(edge.getId(), false);
        WireNode nodeA = getNode(edge.getNodeAId());
        WireNode nodeB = getNode(edge.getNodeBId());
        this.edges.put(edge.getId(), edge);
        this.edgesByHash.put(edge.getHash(), edge);
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
        if (updateClients) {            
            WiresSyncData netData = new WiresSyncData(getId(), null, () -> List.of(edge), () -> List.of(nodeA, nodeB), true);
            for (ServerPlayer player : getPlayersForEdge(edge.getId())) {
                DataAccessor.getFromClient(player, new WiresSyncData.Wrapper(netData), NetworkManager.WIRE_CONNECTOR_DATA_TRANSFER, $ -> {});
            }
        }

        setDirty();
    }
    
    public void sendEdgeToClient(WireEdge edge) {        
        WireNode nodeA = getNode(edge.getNodeAId());
        WireNode nodeB = getNode(edge.getNodeBId());
        WiresSyncData netData = new WiresSyncData(getId(), null, () -> List.of(edge), () -> List.of(nodeA, nodeB), true);
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

        if (removePosition != null && player != null) {
            this.edges.get(id).onRemove(level, removePosition, player);
        }

        removeEdgeInternal(id, true);
        setDirty();
    }

    /**
     * 
     * @param id
     * @param deleteEmptyNodes
     * @return The last assigned edge
     */
    protected synchronized Optional<WireEdge> removeEdgeInternal(UUID id, boolean deleteEmptyNodes) {
        WireEdge edge = edges.remove(id);
        if (edge == null) {
            return Optional.empty();
        }

        edgesByNode.values().removeIf(x -> x.equals(edge));
        edgesByHash.values().removeIf(x -> x.equals(edge));
        NewWireCollision collision = collisionById.remove(id);
        collisionByChunk.values().removeIf(x -> x == collision);
        collisionBySection.values().removeIf(x -> x == collision);
        collisionByBlock.values().removeIf(x -> x == collision);

        removeEdgeFromNode(edge, edge.getNodeAId(), deleteEmptyNodes);
        removeEdgeFromNode(edge, edge.getNodeBId(), deleteEmptyNodes);

        return Optional.of(edge);
    }

    protected synchronized void removeEdgeFromNode(WireEdge edge, UUID nodeId, boolean deleteEmptyNode) {
        if (!edge.getNodeAId().equals(nodeId) && !edge.getNodeBId().equals(nodeId)) {
            throw new IllegalStateException("Node " + nodeId + " is not part of edge " + edge.getId());
        }
        if (!getNode(nodeId).removeConnection(edge.getId()) && deleteEmptyNode) {
            if (ModCommonConfig.WIRE_CONVERTER_LOGGING.get()) PantographsAndWires.LOGGER.info("[GRAPH CONVERTER/UPDATER]                - REMOVE REPLACED NODE: " + nodeId);
            removeNode(nodeId, null, null);
        }
    }

    protected synchronized void replaceNodeInEdge(WireEdge edge, UUID oldNodeId, UUID newNodeId, boolean deleteEmptyNode) {
        boolean isA = edge.getNodeAId().equals(oldNodeId);
        removeEdgeFromNode(edge, oldNodeId, deleteEmptyNode);
        if (isA) {
            edge.swapNodes(newNodeId, edge.getNodeBId());
        } else {
            edge.swapNodes(edge.getNodeAId(), newNodeId);
        }
        if (ModCommonConfig.WIRE_CONVERTER_LOGGING.get()) PantographsAndWires.LOGGER.info("[GRAPH CONVERTER/UPDATER]            - EDGE NODES MODIFIED: " + oldNodeId + " -> " + newNodeId + ", is point A? " + isA + ", Edge Id: " + edge.getId());
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
        if (!collisionById.containsKey(edgeId)) {
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
    protected WireNode updateNodeData(WireNode node) {
        WireNode newNode = node.getData().updateWireNode(this, node);
        if (newNode == null) {
            return node;
        }
        boolean swapNode = !node.getId().equals(newNode.getId());
        if (swapNode) {
            if (ModCommonConfig.WIRE_CONVERTER_LOGGING.get()) PantographsAndWires.LOGGER.info("[GRAPH CONVERTER/UPDATER]    - NODE SWAPPED: " + node.getId() + " -> " + newNode.getId() + ", with connections: " + node.getConnections().size());
        }

        Iterator<UUID> ids = new ArrayList<>(node.getConnections()).iterator();
        while (ids.hasNext()) {
            UUID id = ids.next();
            if (!hasEdge(id)) {
                node.removeConnection(id);
                continue;
            }
            
            WireEdge edge = getEdge(id);            
            if (swapNode) {
                replaceNodeInEdge(edge, node.getId(), newNode.getId(), true);
            }
            WireNode nodeA = getNode(edge.getNodeAId());
            WireNode nodeB = getNode(edge.getNodeBId());
            CustomData customData = edge.getWireConnectionData().customData();
            edge.setWireConnectionData(new WireConnectionData(
                customData,
                nodeA.getData().getConnectorCustomData(this, customData, nodeA, 0).orElse(edge.getWireConnectionData().connectorA()),
                nodeB.getData().getConnectorCustomData(this, customData, nodeB, 1).orElse(edge.getWireConnectionData().connectorB())
            ));
            setEdge(edge, true);
            setDirty();
            
        }
        return newNode;
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
                removeEdge(connection, new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f), player);
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
        if (level.isClientSide()) return;

        playersWatchingChunk.put(pos, player.getUUID());
        
        synchronized (nodesByChunk) {
            if (nodesByChunk.containsKey(pos) && player instanceof ServerPlayer serverPlayer) {
                Set<UUID> edgeIds = new LinkedHashSet<>();
                Collection<UUID> nodeIds = ImmutableList.copyOf(nodesByChunk.get(pos));
                for (UUID nodeId : nodeIds) {
                    if (!hasNode(nodeId)) {
                        continue;
                    }
                    WireNode node = updateNodeData(getNode(nodeId));
                    if (ModCommonConfig.WIRE_CONVERTER_LOGGING.get()) PantographsAndWires.LOGGER.info("[GRAPH CONVERTER/UPDATER] - NODE " + nodeId + ": " + node.getPos().x + ", " + node.getPos().y + ", " + node.getPos().z);

                    if (!node.getData().validate(this, new CompoundTag(), 0)) {
                        removeNode(nodeId, null, null);
                        PantographsAndWires.LOGGER.warn("Removed wire node with id {} at {}, because it is no longer valid.", node.getId(), node.getPos());
                        continue;
                    }

                    for (UUID connectionId : node.getConnections()) {
                        edgeIds.add(connectionId);
                    }
                }
                if (edgeIds.isEmpty()) {
                    return;
                }

                Collection<WireEdge> edges = new ArrayList<>(edgeIds.size());
                Set<WireNode> nodes = new HashSet<>();
                for (UUID id : edgeIds) {
                    WireEdge edge = getEdge(id);
                    if (edge == null) continue;
                    edges.add(edge);
                    nodes.add(getNode(edge.getNodeAId()));
                    nodes.add(getNode(edge.getNodeBId()));
                }
                DataAccessor.getFromClient(serverPlayer, new WiresSyncData.Wrapper(new WiresSyncData(getId(), pos, () -> edges, () -> nodes, true)), NetworkManager.WIRE_CONNECTOR_DATA_TRANSFER, $ -> {});
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


    
    private static record PrimitiveNode(UUID id, BlockConnectorNodeData nodeData, Vector3f pos) {}  
    private static record PrimitiveEdge(PrimitiveNode nodeA, PrimitiveNode nodeB, IWireType type, CompoundTag customData) {}  

    public final void upgrade(CompoundTag nbt) {
        final int steps = 2;
        long startTime = 0;
        PantographsAndWires.LOGGER.info("Converting wire data for dimension " + level.dimension().location() + "! This process may take a moment. Please wait...");

        // STEP 1
        PantographsAndWires.LOGGER.info("[WIRE CONVERSION] [STEP 1/" + steps + "]: Reading and processing legacy data...");
        startTime = System.currentTimeMillis();

        List<CompoundTag> connectionsList = nbt.getList("Connections", Tag.TAG_COMPOUND).stream().map(x -> (CompoundTag)x).toList();
        List<PrimitiveEdge> edges = new ArrayList<>();
        int nodesCount = 0;

        for (CompoundTag connection : connectionsList) {
            String wireId = connection.getString("WireType");
            IWireType type = switch (wireId) {
                case "pantographsandwires:catenary_wire" -> ModWireRegistry.CATENARY_WIRE;
                case "pantographsandwires:energy_wire" -> ModWireRegistry.ENERGY_WIRE;
                default -> null;
            };
            if (type == null) {
                PantographsAndWires.LOGGER.warn("[WIRE CONVERSION] Unknown wire type id '" + type + "''. This connection is skipped!");
                continue;
            }

            CompoundTag customData = new CompoundTag();
            if (wireId.equals("pantographsandwires:catenary_wire")) {
                CompoundTag customDataNbt = connection.getCompound("CreationData");

                int cantileverAIndex = customDataNbt.getBoolean("ClickedRight1") ? 1 : 0;
                int cantileverBIndex = customDataNbt.getBoolean("ClickedRight2") ? 1 : 0;
                if (cantileverAIndex > 0 || cantileverBIndex > 0) {
                    CompoundTag customPointData = new CompoundTag();
                    if (cantileverAIndex > 0) {
                        CompoundTag pointNbt = new CompoundTag();
                        pointNbt.putInt("CantileverIndex", cantileverAIndex);
                        customPointData.put("0", pointNbt);
                    }
                    if (cantileverBIndex > 0) {
                        CompoundTag pointNbt = new CompoundTag();
                        pointNbt.putInt("CantileverIndex", cantileverBIndex);
                        customPointData.put("1", pointNbt);
                    }
                    customData.put("CustomPointData", customPointData);
                }
            }
            BlockPos nodeAPos = Utils.getNbtBlockPos(connection, "PosA");
            BlockPos nodeBPos = Utils.getNbtBlockPos(connection, "PosB");
            PrimitiveNode nodeA = new PrimitiveNode(UUID.randomUUID(), new BlockConnectorNodeData(nodeAPos), new Vector3f(nodeAPos.getX(), nodeAPos.getY(), nodeAPos.getZ()));
            PrimitiveNode nodeB = new PrimitiveNode(UUID.randomUUID(), new BlockConnectorNodeData(nodeBPos), new Vector3f(nodeBPos.getX(), nodeBPos.getY(), nodeBPos.getZ()));
            nodesCount++;
            edges.add(new PrimitiveEdge(nodeA, nodeB, type, customData));
        }

        PantographsAndWires.LOGGER.info("[WIRE CONVERSION] [STEP 1 SUCCESS]: Found " + nodesCount + " nodes and " + edges.size() + " edges. Took " + (System.currentTimeMillis() - startTime) + "ms");        
        // STEP 2
        PantographsAndWires.LOGGER.info("[WIRE CONVERSION] [STEP 2/" + steps + "]: Converting data..");
        startTime = System.currentTimeMillis();

        for (PrimitiveEdge edge : edges) {
            MutableInt i = new MutableInt();
            createEdge(edge.type(), new CustomData(edge.customData()), edge.nodeA.nodeData(), edge.nodeB.nodeData(), i, false);
        }

        PantographsAndWires.LOGGER.info("[WIRE CONVERSION] [STEP 2 SUCCESS]: Took " + (System.currentTimeMillis() - startTime) + "ms");
        PantographsAndWires.LOGGER.info("[WIRE CONVERSION] [STATUS]: " + getStatistics().print(true));
    }
}
