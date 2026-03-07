package de.mrjulsen.paw.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import de.mrjulsen.paw.compat.sodium.MeshAppenderRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;

/**
 * Fix invisible wires with sodium
 */
@Mixin(ChunkBuilderMeshingTask.class)
public class ChunkBuilderMeshingTaskMixin {

    @Shadow
    private RenderSection render;

    @Shadow
    private ChunkRenderContext renderContext;

    @Shadow
    private ReportedException fillCrashInfo(CrashReport report, WorldSlice slice, BlockPos pos) {
        throw new AssertionError();
    }

    private WorldSlice slice;
    
    @ModifyVariable(method = "execute", at = @At(value = "STORE"), remap = false)
    public WorldSlice getWorldSlice(WorldSlice slice) {
        this.slice = slice;
        return slice;
    }

    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Reference2ReferenceOpenHashMap;<init>()V", shift = Shift.BEFORE), remap = false)
    public void append(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(minX, minY, minZ);
        ChunkBuildBuffers buffers = buildContext.buffers;
        try {
            MeshAppenderRenderer.renderMeshAppenders(slice, renderContext.getOrigin(), buffers);
        } catch (ReportedException var24) {
            throw fillCrashInfo(var24.getReport(), slice, blockPos);
        } catch (Exception var25) {
            throw fillCrashInfo(CrashReport.forThrowable(var25, "Encountered exception while building chunk meshes"), slice, blockPos);
        } finally {
            this.slice = null;
        }
    }
}
