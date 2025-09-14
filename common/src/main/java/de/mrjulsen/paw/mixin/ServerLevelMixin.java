package de.mrjulsen.paw.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.mrjulsen.wires.graph.WireGraph;
import de.mrjulsen.wires.graph.WireGraphManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    
	@Inject(method = "sendBlockUpdated", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
	public void wireBlockCallback(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
		Level level = (Level)(Object)this;
		for (WireGraph graph : WireGraphManager.getAll(level)) {
			graph.notifyBlockUpdate(level, Optional.empty(), pos, newState, flags);
		}
	}
}