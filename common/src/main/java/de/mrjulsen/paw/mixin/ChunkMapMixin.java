package de.mrjulsen.paw.mixin;

import net.neoforged.neoforge.event.EventHooks;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.mrjulsen.paw.event.ChunkLoadingEvents;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Inject(method = "markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("TAIL"))
    private static void paw$markChunkPendingToSend(ServerPlayer player, LevelChunk chunk, CallbackInfo ci) {
        ChunkLoadingEvents.fireChunkWatch(false, true, player, chunk.getPos(), player.serverLevel());
    }

    @Inject(method = "dropChunk", at = @At("HEAD"))
    private static void paw$dropChunk(ServerPlayer player, ChunkPos chunkPos, CallbackInfo ci) {
        ChunkLoadingEvents.fireChunkWatch(true, false, player, chunkPos, player.serverLevel());
    }
}
