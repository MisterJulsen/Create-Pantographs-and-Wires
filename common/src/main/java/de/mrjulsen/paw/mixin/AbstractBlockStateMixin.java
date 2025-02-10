package de.mrjulsen.paw.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.mrjulsen.wires.WireNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AbstractBlockStateMixin {
	@Inject(method = "entityInside", at = @At(value = "HEAD"))
	public void onBlockCollision(Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
		WireNetwork.get(level).checkEntityCollision(level, pos, entity);
	}
}

