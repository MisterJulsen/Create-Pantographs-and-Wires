package de.mrjulsen.paw.mixin.datafixer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.datafixers.DataFixer;

import de.mrjulsen.paw.datafixer.api.DataFixesInternals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;

@Mixin(DataFixTypes.class)
public class DataFixTypesMixin {
    @Inject(
        method = "update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/CompoundTag;II)Lnet/minecraft/nbt/CompoundTag;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void updateDataWithFixers(DataFixer fixer, CompoundTag tag, int version, int newVersion, CallbackInfoReturnable<CompoundTag> cir) {
        cir.setReturnValue(DataFixesInternals.get().updateWithAllFixers((DataFixTypes) (Object) this, cir.getReturnValue()));
    }
}
