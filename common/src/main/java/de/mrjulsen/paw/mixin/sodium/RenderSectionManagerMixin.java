package de.mrjulsen.paw.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.ClientUtils;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSectionManager.class)
public class RenderSectionManagerMixin {

    @Unique
    private final ThreadLocal<SectionPos> dragonlib$section = new ThreadLocal<>();

    @Inject(method = "onSectionAdded", at = @At(value = "HEAD"), remap = false)
    public void dragonlib$getSection(int x, int y, int z, CallbackInfo ci) {
        dragonlib$section.set(SectionPos.of(x, y, z));
    }

    @WrapOperation(method = "onSectionAdded", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;hasOnlyAir()Z", remap = true), remap = false)
    public boolean dragonlib$emptyCheck(LevelChunkSection instance, Operation<Boolean> original) {
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
