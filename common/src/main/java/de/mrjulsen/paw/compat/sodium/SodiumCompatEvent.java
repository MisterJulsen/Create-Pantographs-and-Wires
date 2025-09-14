package de.mrjulsen.paw.compat.sodium;

import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.render.WireRenderer;
import de.mrjulsen.wires.util.ClientUtils;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public class SodiumCompatEvent {
    public static final Event<MeshAppender> CHUNK_MESHING_EVENT = EventFactory.createEventResult();

    public static void init() {
        SodiumCompatEvent.CHUNK_MESHING_EVENT.register(c -> {
            for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
                if (graph.hasConnectionsInSection(c.sectionOrigin())) {
                    WireRenderer.renderConnectionsInSection(c.vertexConsumerProvider(), c.sodiumBuildBuffers(), c.blockRenderView(), c.sectionOrigin());
                    break;
                }
            }
        });
    }
}
