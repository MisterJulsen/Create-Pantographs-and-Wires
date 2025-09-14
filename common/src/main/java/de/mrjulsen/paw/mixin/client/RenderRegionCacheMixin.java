package de.mrjulsen.paw.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.ClientUtils;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderRegionCache.ChunkInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/*
 * Fixes rendering of wires in empty chunks
 */
@Mixin(RenderRegionCache.class)
public class RenderRegionCacheMixin {
    
    @Inject(method = "isAllEmpty", at = @At(value = "HEAD"), cancellable = true)
    private static void isEmpty(BlockPos start, BlockPos end, int sectionX, int sectionZ, ChunkInfo[][] chunkInfos, CallbackInfoReturnable<Boolean> cir) {
        int sectionY = SectionPos.blockToSectionCoord(start.getY());
        SectionPos pos = SectionPos.of(sectionX + 1, sectionY + 1, sectionZ + 1);
        for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
            if (graph.hasConnectionsInSection(pos)) {
                cir.setReturnValue(false);
                break;
            }
        }
    }
}
