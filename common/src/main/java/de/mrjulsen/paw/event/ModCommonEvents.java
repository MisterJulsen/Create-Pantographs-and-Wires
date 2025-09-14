package de.mrjulsen.paw.event;

import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.LifecycleEvent;

public final class ModCommonEvents {

    private ModCommonEvents() {}
    
    public static void init() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register((level) -> {
            WireGraphManager.build(level);
        });

        LifecycleEvent.SERVER_STOPPED.register((server) -> {
            WireGraphManager.clearServer();
        });

        BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
            if (level.isClientSide) {
                return EventResult.pass();
            }

            for (WireGraph graph : WireGraphManager.getAll(level)) {
                WiresApi.GENERIC_BLOCK.getAccessor(graph).ifPresent(x -> {
                    for (WireNode node : x.get(pos)) {
                        graph.removeNode(node.getId(), new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f), Optional.of(player));
                    }
                });                
            }
            return EventResult.pass();
        });
    }
}
