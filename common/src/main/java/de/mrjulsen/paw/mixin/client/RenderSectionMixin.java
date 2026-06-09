package de.mrjulsen.paw.mixin.client;

import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.ClientUtils;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public class RenderSectionMixin {

    @Final
    @Shadow
    BlockPos.MutableBlockPos origin;

    @ModifyArg(method = "createCompileTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderRegionCache;createRegion(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/SectionPos;Z)Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;"))
    public boolean paw$isEmpty(boolean arg) {
        SectionPos section = SectionPos.of(this.origin);
        for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
            if (graph.hasConnectionsInSection(section)) {
                return false;
            }
        }
        return arg;
    }
}
