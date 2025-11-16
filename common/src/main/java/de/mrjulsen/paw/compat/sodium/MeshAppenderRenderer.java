package de.mrjulsen.paw.compat.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import dev.architectury.injectables.annotations.ExpectPlatform;

public class MeshAppenderRenderer {

    @ExpectPlatform
    public static void renderMeshAppenders(BlockAndTintGetter world, SectionPos origin, ChunkBuildBuffers buffers) {
        throw new AssertionError();
    }
}