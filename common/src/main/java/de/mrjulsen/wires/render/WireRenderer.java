package de.mrjulsen.wires.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.paw.mixin.client.RenderChunkAccess;
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.ClientUtils;
import de.mrjulsen.wires.util.CompiledChunkExtension;
import de.mrjulsen.wires.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;

public class WireRenderer implements ResourceManagerReloadListener {

	public static final Cache<TextureAtlasSprite> WIRE_TEXTURE = new Cache<>(
			() -> Minecraft.getInstance().getModelManager()
					.getAtlas(InventoryMenu.BLOCK_ATLAS)
					.getSprite(Utils.resLoc(WiresApi.MOD_ID, "block/wire")));

	@Override
	public void onResourceManagerReload(@Nonnull ResourceManager pResourceManager) {
		WIRE_TEXTURE.clear();
	}

	public static void renderConnectionsInSection(Set<RenderType> layers, ChunkBufferBuilderPack buffers, BlockAndTintGetter region, RenderChunk renderChunk) {
		BlockPos chunkOrigin = renderChunk.getOrigin();
		SectionPos chunkSection = SectionPos.of(chunkOrigin);

		for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
			if (!graph.hasConnectionsInSection(chunkSection)) continue;

			RenderType renderType = RenderType.cutout();
			BufferBuilder builder = buffers.builder(renderType);
			if (layers.add(renderType)) {
				((RenderChunkAccess) renderChunk).invokeBeginLayer(builder);
			}

			renderConnectionsInternal(graph, builder, region, chunkSection, new PoseStack());

			CompiledChunkExtension ext = (CompiledChunkExtension) renderChunk.compiled.get();
			ext.setHasWires(true);
			
			Collection<WireSegmentRenderDataBatch> connections = graph.connectionsInSection(chunkSection);
			if (layers.add(renderType)) {
				((RenderChunkAccess) renderChunk).invokeBeginLayer(builder);
			}
			for (WireSegmentRenderDataBatch connection : connections) {
				connection.render(region, builder);
			}
		}
	}

	public static void renderConnectionsInSection(Function<RenderType, VertexConsumer> layers, me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers buffers, BlockAndTintGetter region, SectionPos section) {
		for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
			if (!graph.hasConnectionsInSection(section)) continue;

			RenderType renderType = RenderType.cutout();
			VertexConsumer vertexConsumer = layers.apply(renderType);
			renderConnectionsInternal(graph, vertexConsumer, region, section, new PoseStack());
		}
	}

	private static void renderConnectionsInternal(WireGraphClient graph, VertexConsumer vertexConsumer, BlockAndTintGetter region, SectionPos section, PoseStack poseStack) {
		Collection<WireSegmentRenderDataBatch> connections = graph.connectionsInSection(section);
		synchronized (connections) {
			for (WireSegmentRenderDataBatch connection : connections) {
				connection.render(region, vertexConsumer);
			}
		}
	}
}
