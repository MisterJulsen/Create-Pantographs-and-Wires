package de.mrjulsen.paw.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.mrjulsen.wires.WireClientNetwork;
import de.mrjulsen.wires.util.ClientUtils;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunkSection;

@Mixin(RenderSectionManager.class)
public class RenderSectionManagerMixin {
    
    private SectionPos section;

    @Inject(method = "onSectionAdded", at = @At(value = "HEAD"), remap = false)
    public void getSection(int x, int y, int z, CallbackInfo ci) {
        this.section = SectionPos.of(x, y, z);
    }

    @Redirect(method = "onSectionAdded", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;hasOnlyAir()Z", remap = true), remap = false)
    public boolean emptyCheck(LevelChunkSection section) {
        return section.hasOnlyAir() && !WireClientNetwork.get(ClientUtils.level()).hasConnectionsInSection(this.section);
    }
}
