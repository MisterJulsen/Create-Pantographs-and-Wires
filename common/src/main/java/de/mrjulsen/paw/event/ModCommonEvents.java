package de.mrjulsen.paw.event;

import java.io.IOException;
import java.util.Optional;

import org.joml.Vector3f;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.mixin.DimensionDataStorageAccessor;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.graph.WireNode;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.nbt.CompoundTag;

public final class ModCommonEvents {

    private ModCommonEvents() {}
    
    public static void init() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register((level) -> {
            if (level.isClientSide()) return;
            WireGraphManager.build(level);
            try {
                CompoundTag nbt = level.getDataStorage().readTagFromDisk("wiresapi_wire_network", 0);
                WireGraphManager.get(level, WiresApi.PAW_CATENARY_WIRES).upgrade(nbt.getCompound("data"));
                level.getDataStorage().save();
                ((DimensionDataStorageAccessor)level.getDataStorage()).paw$getDataFile("wiresapi_wire_network").delete();
            } catch (IOException e) {
                PantographsAndWires.LOGGER.error("Could not convert legacy wire data.", e);
            }
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
