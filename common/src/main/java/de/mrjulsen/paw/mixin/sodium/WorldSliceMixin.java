package de.mrjulsen.paw.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.ClientUtils;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;

@Mixin(WorldSlice.class)
public class WorldSliceMixin {
    
    private static SectionPos section;

    @Inject(method = "prepare", at = @At(value = "HEAD"), remap = false)
    private static void getSection(Level world, SectionPos origin, ClonedChunkSectionCache sectionCache, CallbackInfoReturnable<ChunkRenderContext> cir) {
        section = origin;
    }

    @Redirect(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;hasOnlyAir()Z", remap = true), remap = false)
    private static boolean emptyCheck(LevelChunkSection chunk) {
        if (!chunk.hasOnlyAir()) {
            return false;
        }
        for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
            if (graph.hasConnectionsInSection(section)) {
                return false;
            }
        }
        return true;
    }
}
