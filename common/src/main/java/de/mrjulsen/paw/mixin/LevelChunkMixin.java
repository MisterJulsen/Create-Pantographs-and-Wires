package de.mrjulsen.paw.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import de.mrjulsen.wires.block.IBlockEntityExtension;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {
    
    @Inject(method = "clearAllBlockEntities", at = @At(value = "HEAD"))
    public void paw$onClearAllBlockEntities(CallbackInfo ci) {   
        LevelChunk self = (LevelChunk)(Object)this;
        for (BlockEntity be : self.getBlockEntities().values()) {
            if (be instanceof IBlockEntityExtension bext) {
                bext.onChunkUnloaded();
            }
        }
    }
    
    @Inject(method = "addAndRegisterBlockEntity", at = @At(value = "INVOKE", shift = Shift.AFTER, target = "Lnet/minecraft/world/level/chunk/LevelChunk;updateBlockEntityTicker(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    public void paw$onAddAndRegisterBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {   
        LevelChunk self = (LevelChunk)(Object)this;
        for (BlockEntity be : self.getBlockEntities().values()) {
            if (be instanceof IBlockEntityExtension bext) {
                bext.onBlockEntityLoad();
            }
        }
    }
}
