package de.mrjulsen.wires;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.mrjulsen.wires.network.WireChunkLoadingData;
import de.mrjulsen.wires.network.WiresNetworkSyncData.WireSyncDataEntry;
import de.mrjulsen.wires.render.WireSegmentRenderDataBatch;
import de.mrjulsen.wires.WireCollision.WireBlockCollision;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class WireClientNetwork implements IWireNetwork {

    private static ResourceLocation dimension;
    private static WireClientNetwork currentNetwork;
    private final Level level;

    protected WireClientNetwork(Level level) {
        this.level = level;
    }
    
    private final Multimap<ChunkPos, WireCollision> collisionByChunk = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Multimap<SectionPos, WireCollision> collisionBySection = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Multimap<BlockPos, WireCollision> collisionByBlock = MultimapBuilder.hashKeys().hashSetValues().build();
    
    private final Multimap<UUID, WireSegmentRenderDataBatch> renderDataById = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Multimap<ChunkPos, WireSegmentRenderDataBatch> renderDataByChunk = MultimapBuilder.hashKeys().hashSetValues().build();
    private final Multimap<SectionPos, WireSegmentRenderDataBatch> renderDataBySection = MultimapBuilder.hashKeys().hashSetValues().build();

    public String debug_text() {
        return String.format("Wires[C]: Col: [%s,%s,%s], R: [%s,%s,%s]",
            collisionByChunk.size(),
            collisionBySection.size(),
            collisionByBlock.size(),

            renderDataById.size(),
            renderDataBySection.size(),
            renderDataByChunk.size()
        );
    }

    @Override
    public Level level() {
        return level;
    }

    public static void clear() {        
        dimension = null;
        currentNetwork = null;
    }
    
    public static WireClientNetwork get(Level level) {
        if (dimension != level.dimensionTypeId().location() || currentNetwork == null) {
            dimension = level.dimensionTypeId().location();
            return currentNetwork = new WireClientNetwork(level);
        }
        return currentNetwork;
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

    public synchronized boolean hasConnectionsInSection(SectionPos section) {
        return renderDataBySection.containsKey(section);
    }

    public synchronized boolean hasConnectionsInBlock(BlockPos pos) {
        return collisionByBlock.containsKey(pos);
    }

    public synchronized Collection<WireSegmentRenderDataBatch> connectionsInSection(SectionPos section) {
        if (!hasConnectionsInSection(section)) {
            return List.of();
        }
        return renderDataBySection.get(section);
    }

    public void createClientConnection(@Nullable ChunkPos chunk, WireSyncDataEntry in) {
        if (in.forceUpdate()) {
            removeClientConnection(in.data().getConnectionId());
        } else if (renderDataById.containsKey(in.data().getConnectionId())) {
            if (chunk != null && renderDataByChunk.containsKey(chunk)) {
                for (WireSegmentRenderDataBatch renderdata : renderDataByChunk.get(chunk)) {
                    renderdata.setUnloaded(false);
                }
            }
            return;
        }
        
        Set<SectionPos> sectionsIn = new HashSet<>();
        IWireType renderer = WireTypeRegistry.get(in.data().getWireType());

        WireBatch batch = renderer.buildWire(WireCreationContext.BOTH, Minecraft.getInstance().level, in.data());
        batch.splitRenderDataInChunkSections(in.data().getConnectionId(), in.data().getOriginChunkSection()).entrySet().forEach(x -> {
            renderDataByChunk.put(x.getKey().chunk(), x.getValue());
            renderDataBySection.put(x.getKey(), x.getValue());  
            renderDataById.put(in.data().getConnectionId(), x.getValue());
            sectionsIn.add(x.getKey());
        });

        new WireCollision(collisionByChunk, collisionBySection, collisionByBlock, in.data().getConnectionId(), in.data().getStartBlockPos(), batch.getCollisions());

        for (SectionPos section : sectionsIn) {
            setSectionDirty(section);
        }
    }

    public void removeClientConnections(UUID[] connectionIds) {
        for (UUID id : connectionIds) {
            removeClientConnection(id);
        }
    }

    public void removeClientConnection(UUID connectionId) {
        if (!renderDataById.containsKey(connectionId)) {
            return;
        }

        Collection<WireSegmentRenderDataBatch> renderdata = renderDataById.removeAll(connectionId);
        renderDataBySection.values().removeAll(renderdata);
        renderDataByChunk.values().removeAll(renderdata);
        collisionByBlock.values().removeIf(x -> x.getId().equals(connectionId));
        collisionByChunk.values().removeIf(x -> x.getId().equals(connectionId));
        collisionBySection.values().removeIf(x -> x.getId().equals(connectionId));
        
        for (WireSegmentRenderDataBatch batch : renderdata) {
            SectionPos section = batch.getSection();
            setSectionDirty(section);
        }
    }

    public void onClientChunkLoading(WireChunkLoadingData in) {
        synchronized (renderDataByChunk) {
            Set<UUID> emptyConnections = new HashSet<>();
            for (WireSegmentRenderDataBatch renderdata : renderDataByChunk.get(in.pos())) {
                renderdata.setUnloaded(!in.load());
                if (!renderDataById.containsKey(renderdata.getId()) || renderDataById.get(renderdata.getId()).stream().allMatch(WireSegmentRenderDataBatch::isUnloaded)) {
                    emptyConnections.add(renderdata.getId());
                }
            }

            for (UUID id : emptyConnections) {                
                removeClientConnection(id);
            }
        }
    }

    private void setSectionDirty(SectionPos pos) {
        Minecraft.getInstance().execute(() -> {            
            Minecraft.getInstance().levelRenderer.setSectionDirty(pos.getX(), pos.getY(), pos.getZ());
        });
    }
   
}
