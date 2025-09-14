package de.mrjulsen.paw.event;

import de.mrjulsen.wires.graph.WireGraphManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class ChunkLoadingEvents {
    public static void fireChunkWatch(boolean watch, ServerPlayer entity, ChunkPos chunkpos, ServerLevel level) {
        if (watch)
            onChunkWatch(level, chunkpos, entity);
        else
            onChunkUnWatch(level, chunkpos, entity);
    }

    public static void fireChunkWatch(boolean wasLoaded, boolean load, ServerPlayer entity, ChunkPos chunkpos, ServerLevel level) {
        if (wasLoaded != load)
            fireChunkWatch(load, entity, chunkpos, level);
    }

    public static void onChunkWatch(Level level, ChunkPos pos, Player player) {
        WireGraphManager.getAll(level).forEach(x -> x.onChunkLoad(level, pos, player));
    }

    public static void onChunkUnWatch(Level level, ChunkPos pos, Player player) {
        WireGraphManager.getAll(level).forEach(x -> x.onChunkUnload(level, pos, player));
    }
}
