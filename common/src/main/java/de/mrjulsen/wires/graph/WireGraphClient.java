package de.mrjulsen.wires.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector3f;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

import de.mrjulsen.wires.Wire;
import de.mrjulsen.wires.WireBatch;
import de.mrjulsen.wires.WireCreationContext;
import de.mrjulsen.wires.graph.data.accessor.NodeAccessor;
import de.mrjulsen.wires.network.WireChunkLoadingData;
import de.mrjulsen.wires.render.WireSegmentRenderDataBatch;
import de.mrjulsen.wires.util.GraphId;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class WireGraphClient implements IWireGraph {

    public static record DebugWireData(String name, Vector3f centerPos) {}

    private final GraphId id;
    private final Level level;
    private final Map<UUID, WireNode> nodes = new HashMap<>();
    private final Map<UUID, WireEdge> edges = new HashMap<>();

    
    private final Map<ResourceLocation, NodeAccessor<?>> nodesByType = new ConcurrentHashMap<>();

    private final Multimap<WireNode, WireEdge> edgesByNode = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    
    // Collision
    final Map<UUID, NewWireCollision> collisionById = new HashMap<>();
    final Multimap<BlockPos, NewWireCollision> collisionByBlock = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    final Multimap<ChunkPos, NewWireCollision> collisionByChunk = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    final Multimap<SectionPos, NewWireCollision> collisionBySection = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    
    // Client Rendering
    final Multimap<UUID, WireSegmentRenderDataBatch> renderDataById = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    final Multimap<ChunkPos, WireSegmentRenderDataBatch> renderDataByChunk = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);
    final Multimap<SectionPos, WireSegmentRenderDataBatch> renderDataBySection = Multimaps.newSetMultimap(new ConcurrentHashMap<>(), ConcurrentHashMap::newKeySet);

    // Debug
    private final Multimap<UUID, DebugWireData> debugWireDataByEdge = MultimapBuilder.hashKeys().linkedListValues().build();

    public WireGraphClient(GraphId id, Level level) {
        this.id = id;
        this.level = level;
    }

    @Override
    public DLStatistics getStatistics() {
        DLStatistics.Group nodesGroup = new DLStatistics.Group("nodes", "N");
        DLStatistics.Group edgesGroup = new DLStatistics.Group("edges", "E");
        DLStatistics.Group collisionsGroup = new DLStatistics.Group("collisions", "C");
        DLStatistics.Group renderingGroup = new DLStatistics.Group("rendering", "R");

        return new DLStatistics("Wires[C]", List.of(
            new DLStatistics.Stat(nodesGroup, "Nodes", nodes.size()),

            new DLStatistics.Stat(edgesGroup, "Edges", edges.size()),
            new DLStatistics.Stat(edgesGroup, "Edges (by node)", edgesByNode.size()),
            new DLStatistics.Stat(edgesGroup, "Debug Wire Data", debugWireDataByEdge.size()),

            new DLStatistics.Stat(collisionsGroup, "Collision", collisionById.size()),
            new DLStatistics.Stat(collisionsGroup, "Collision (by block)", collisionByBlock.size()),
            new DLStatistics.Stat(collisionsGroup, "Collision (by section)", collisionBySection.size()),
            new DLStatistics.Stat(collisionsGroup, "Collision (by chunk)", collisionByChunk.size()),

            new DLStatistics.Stat(renderingGroup, "Rendering", renderDataById.size()),
            new DLStatistics.Stat(renderingGroup, "Rendering (by section)", renderDataBySection.size()),
            new DLStatistics.Stat(renderingGroup, "Rendering (by chunk)", renderDataByChunk.size())            
        ));
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

    private synchronized void setSectionDirty(SectionPos pos) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().levelRenderer.setSectionDirty(pos.getX(), pos.getY(), pos.getZ());
        });
    }




    public void addNode(WireNode node) {
        this.nodes.put(node.getId(), node);
        this.nodesByType.computeIfAbsent(node.getData().getRegistryType().id(), (a) -> node.getData().getRegistryType().createAccessor()).put(node);
    }

    public void removeNode(UUID id) {
        WireNode node = nodes.remove(id);
        this.edgesByNode.removeAll(node);
        this.nodesByType.get(node.getData().getRegistryType().id()).remove(node);
    }

    public WireEdge addEdge(WireEdge edge) {
        WireBatch batch = edge.getType().buildWire(WireCreationContext.BOTH, getLevel(), edge.getWireConnectionData(), edge, getNode(edge.getNodeAId()), getNode(edge.getNodeBId()));
        if (batch == null) {
            return edge;
        }

        removeEdgeInternal(edge.getId(), false);
        WireNode nodeA = getNode(edge.getNodeAId());
        WireNode nodeB = getNode(edge.getNodeBId());
        this.edges.put(edge.getId(), edge);
        this.edgesByNode.put(nodeA, edge);
        this.edgesByNode.put(nodeB, edge);
        nodeA.addConnection(edge.getId());
        nodeB.addConnection(edge.getId());
        
        for (Map.Entry<String, Wire> wire : batch.getWires().entrySet()) {
            this.debugWireDataByEdge.put(edge.getId(), new DebugWireData(wire.getKey(), wire.getValue().pos()));
        }

        // Collisions
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
        
        // Rendering
        Set<SectionPos> sectionsIn = new HashSet<>();
        batch.splitRenderDataInChunkSections(edge.getId(), edge.getDecorations()).forEach((sec, seg) -> {
            renderDataById.put(seg.getId(), seg);
            renderDataByChunk.put(sec.chunk(), seg);
            renderDataBySection.put(sec, seg);
            sectionsIn.add(sec);
        });
        
        for (SectionPos section : sectionsIn) {
            setSectionDirty(section);
        }
        return edge;
    }

    public void removeEdge(UUID id) {
        removeEdgeInternal(id, true);
    }

    private void removeEdgeInternal(UUID id, boolean checkForEmptyNodes) {
        if (!edges.containsKey(id)) {
            return;
        }

        WireEdge edge = edges.remove(id);
        edgesByNode.values().remove(edge);
        debugWireDataByEdge.removeAll(id);

        if (checkForEmptyNodes) {
            if (!getNode(edge.getNodeAId()).removeConnection(id)) {
                removeNode(edge.getNodeAId());
            }
            if (!getNode(edge.getNodeBId()).removeConnection(id)) {
                removeNode(edge.getNodeBId());
            }
        }

        Collection<WireSegmentRenderDataBatch> renderdata = renderDataById.removeAll(id);
        renderDataBySection.values().removeAll(renderdata);
        renderDataByChunk.values().removeAll(renderdata);    

        NewWireCollision collision = collisionById.remove(id);
        collisionByChunk.values().remove(collision);
        collisionBySection.values().remove(collision);
        collisionByBlock.values().remove(collision);
        
        for (WireSegmentRenderDataBatch batch : renderdata) {
            SectionPos section = batch.getSection();
            setSectionDirty(section);
        }
    }

    public synchronized void updateClientEdge(WireEdge edge) {
        removeEdge(edge.getId());
        addEdge(edge);
    }   

    public void onClientChunkLoading(WireChunkLoadingData in) {
        Set<UUID> emptyConnections = new HashSet<>();
        for (WireSegmentRenderDataBatch renderdata : renderDataByChunk.get(in.pos())) {
            renderdata.setUnloaded(!in.load());
            if (!renderDataById.containsKey(renderdata.getId()) || renderDataById.get(renderdata.getId()).stream().allMatch(WireSegmentRenderDataBatch::isUnloaded)) {
                emptyConnections.add(renderdata.getId());
            }
        }

        for (UUID id : emptyConnections) {
            removeEdge(id);
        }
    }


    public Collection<DebugWireData> debug_getWireDataForEdge(UUID edgeId) {
        return debugWireDataByEdge.containsKey(edgeId) ? Collections.unmodifiableCollection(debugWireDataByEdge.get(edgeId)) : List.of();
    }

    

    public boolean hasConnectionsInSection(SectionPos section) {
        return renderDataBySection.containsKey(section);
    }

    public Collection<WireSegmentRenderDataBatch> connectionsInSection(SectionPos section) {
        if (!hasConnectionsInSection(section)) {
            return List.of();
        }
        return Collections.unmodifiableCollection(renderDataBySection.get(section));
    }





    public Collection<NewWireCollision> getCollisionsInChunk(ChunkPos chunk) {
        return Collections.unmodifiableCollection(collisionByChunk.get(chunk));
    }

    public Collection<NewWireCollision> getCollisionsInBlock(BlockPos pos) {
        return Collections.unmodifiableCollection(collisionByBlock.get(pos));
    }

    public Optional<NewWireCollision> getCollisionById(UUID id) {
        return Optional.ofNullable(collisionById.get(id));
    }
    
}
