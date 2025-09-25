package de.mrjulsen.paw.mixin;

import java.io.File;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.level.storage.DimensionDataStorage;

@Mixin(DimensionDataStorage.class)
public interface DimensionDataStorageAccessor {
    @Invoker("getDataFile")
    File paw$getDataFile(String name);
}
