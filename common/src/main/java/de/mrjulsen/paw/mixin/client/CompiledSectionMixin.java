package de.mrjulsen.paw.mixin.client;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.wires.util.CompiledChunkExtension;

import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionRenderDispatcher.CompiledSection.class)
public class CompiledSectionMixin implements CompiledChunkExtension {

	private boolean hasWires = false;

	@Override
	public boolean paw$hasWires() {
		return hasWires;
	}

	@Override
	public void paw$setHasWires(boolean b) {
		this.hasWires = b;
	}

	@Inject(method = "hasNoRenderableLayers", at = @At(value = "RETURN"), cancellable = true)
	public void hasNoRenderableLayers(CallbackInfoReturnable<Boolean> cir) {
		if (paw$hasWires()) {
			cir.setReturnValue(false);
		}
	}
}
