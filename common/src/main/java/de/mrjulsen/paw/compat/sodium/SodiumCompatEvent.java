package de.mrjulsen.paw.compat.sodium;

import de.mrjulsen.wires.WireClientNetwork;
import de.mrjulsen.wires.render.WireRenderer;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public class SodiumCompatEvent {
    public static final Event<MeshAppender> CHUNK_MESHING_EVENT = EventFactory.createEventResult();

    public static void init() {
        SodiumCompatEvent.CHUNK_MESHING_EVENT.register(c -> {
            if (WireClientNetwork.hasConnectionsInSection(c.sectionOrigin())) {
                WireRenderer.renderConnectionsInSection(c.vertexConsumerProvider(), c.sodiumBuildBuffers(), c.blockRenderView(), c.sectionOrigin());
            }
        });
    }
}
