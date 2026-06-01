package de.mrjulsen.paw.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.ClientUtils;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelSlice.class)
public class WorldSliceMixin {

    @Unique
    private static final ThreadLocal<SectionPos> dragonlib$section = new ThreadLocal<>();

    @Inject(method = "prepare", at = @At(value = "HEAD"), remap = false)
    private static void dragonlib$getSection(Level level, SectionPos pos, ClonedChunkSectionCache cache, CallbackInfoReturnable<ChunkRenderContext> cir) {
        dragonlib$section.set(pos);
    }

    @WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;hasOnlyAir()Z", remap = true), remap = false)
    private static boolean dragonlib$emptyCheck(LevelChunkSection instance, Operation<Boolean> original) {
        boolean b = original.call(instance);
        if (b) {
            for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
                if (graph.hasConnectionsInSection(dragonlib$section.get())) {
                    return false;
                }
            }
        }
        return b;
    }
}
