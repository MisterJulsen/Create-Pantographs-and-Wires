package de.mrjulsen.paw.neoforge.compat;

import org.embeddedt.embeddium.api.ChunkMeshEvent;

import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.render.WireRenderer;
import de.mrjulsen.wires.util.ClientUtils;

public class EmbeddiumCompat {
    
    public static void register() {
        ChunkMeshEvent.BUS.addListener(EmbeddiumCompat::meshAppendEvent);
    }
	
    static void meshAppendEvent(ChunkMeshEvent event) {
        for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
            if (graph.hasConnectionsInSection(event.getSectionOrigin())) {
                event.addMeshAppender(c -> {
                    WireRenderer.renderSodiumConnectionsInSection(c.vertexConsumerProvider(), c.blockRenderView(), c.sectionOrigin());
                });
                break;
            }            
        }
        
    }
}
