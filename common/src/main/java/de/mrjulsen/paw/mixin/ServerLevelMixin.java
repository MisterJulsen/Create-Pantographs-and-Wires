package de.mrjulsen.paw.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.mrjulsen.wires.WireNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    
	@Inject(method = "sendBlockUpdated", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
	public void wireBlockCallback(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
		Level level = (Level)(Object)this;
		WireNetwork.get(level).notifyBlockUpdate(level, pos, newState, flags);
	}
}