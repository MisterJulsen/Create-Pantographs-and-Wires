package de.mrjulsen.paw.forge.compat;

import org.embeddedt.embeddium.api.ChunkMeshEvent;

import de.mrjulsen.wires.render.WireRenderer;
import de.mrjulsen.wires.util.ClientUtils;
import de.mrjulsen.wires.WireClientNetwork;

public class EmbeddiumCompat {
    
    public static void register() {
        ChunkMeshEvent.BUS.addListener(EmbeddiumCompat::meshAppendEvent);
    }
	
    static void meshAppendEvent(ChunkMeshEvent event) {
        if (WireClientNetwork.get(ClientUtils.level()).hasConnectionsInSection(event.getSectionOrigin())) {
			event.addMeshAppender(c -> {
                WireRenderer.renderConnectionsInSection(c.vertexConsumerProvider(), c.sodiumBuildBuffers(), c.blockRenderView(), c.sectionOrigin());
            });
        }
    }
}
