package de.mrjulsen.wires.util;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;

public class SafeChunkUtils {
    private static final Map<LevelAccessor, Set<ChunkPos>> unloadingChunks = new WeakHashMap<>();

    public static LevelChunk getSafeChunk(LevelAccessor w, BlockPos pos) {
        ChunkSource provider = w.getChunkSource();
        ChunkPos chunkPos = new ChunkPos(pos);
        if (unloadingChunks.getOrDefault(w, ImmutableSet.of()).contains(chunkPos))
            return null;
        return provider.getChunkNow(chunkPos.x, chunkPos.z);
    }

    public static boolean isChunkSafe(LevelAccessor w, BlockPos pos) {
        return getSafeChunk(w, pos) != null;
    }

    public static BlockEntity getSafeBE(LevelAccessor w, BlockPos pos) {
        LevelChunk c = getSafeChunk(w, pos);
        if (c == null)
            return null;
        else
            return c.getBlockEntity(pos);
    }

    @Nonnull
    public static BlockState getBlockState(LevelAccessor w, BlockPos pos) {
        LevelChunk c = getSafeChunk(w, pos);
        if (c == null)
            return Blocks.AIR.defaultBlockState();
        else
            return c.getBlockState(pos);
    }

    public static int getRedstonePower(Level w, BlockPos pos, Direction d) {
        if (!isChunkSafe(w, pos))
            return 0;
        else
            return w.getSignal(pos, d);
    }

    public static void onTick(ServerLevel level) {
        unloadingChunks.remove(level);
    }
}
