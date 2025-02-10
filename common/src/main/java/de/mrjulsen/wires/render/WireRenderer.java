package de.mrjulsen.wires.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.paw.mixin.client.RenderChunkAccess;
import de.mrjulsen.wires.WireClientNetwork;
import de.mrjulsen.wires.WiresApi;
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
			.getSprite(Utils.resLoc(WiresApi.MOD_ID, "block/wire"))
	);

	@Override
	public void onResourceManagerReload(@Nonnull ResourceManager pResourceManager) {
		WIRE_TEXTURE.clear();		
	}

	public static void renderConnectionsInSection(Set<RenderType> layers, ChunkBufferBuilderPack buffers, BlockAndTintGetter region, RenderChunk renderChunk) {
		BlockPos chunkOrigin = renderChunk.getOrigin();
		SectionPos chunkSection = SectionPos.of(chunkOrigin);
		if (!WireClientNetwork.get(ClientUtils.level()).hasConnectionsInSection(chunkSection)) {
			return;
		}

		RenderType renderType = RenderType.solid();
		BufferBuilder builder = buffers.builder(renderType);
		if (layers.add(renderType)) {
			((RenderChunkAccess)renderChunk).invokeBeginLayer(builder);		
		}

		renderConnectionsInternal(builder, region, chunkSection, new PoseStack());
		
		CompiledChunkExtension ext = (CompiledChunkExtension)renderChunk.compiled.get();
		ext.setHasWires(true);
	}

	public static void renderConnectionsInSection(Function<RenderType, VertexConsumer> layers, me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers buffers, BlockAndTintGetter region, SectionPos section) {
		if (!WireClientNetwork.get(ClientUtils.level()).hasConnectionsInSection(section)) {
			return;
		}
		RenderType renderType = RenderType.solid();
		VertexConsumer vertexConsumer = layers.apply(renderType);
		renderConnectionsInternal(vertexConsumer, region, section, new PoseStack());
	}

	private static void renderConnectionsInternal(VertexConsumer vertexConsumer, BlockAndTintGetter region, SectionPos section, PoseStack poseStack) {
		Collection<WireSegmentRenderDataBatch> connections = WireClientNetwork.get(ClientUtils.level()).connectionsInSection(section);

		for (WireSegmentRenderDataBatch connection : connections) {
			connection.render(vertexConsumer);

			/*
			for (WireSegmentRenderData segment : connection.getSubWireSegments()) {	
				Vec3 point = segment.getPoint(0).vertex(VertexCorner.CENTER);
				BlockPos pos = chunkOrigin.offset(point.x(), point.y(), point.z());
				poseStack.pushPose();
				poseStack.translate((double)(chunkOrigin.getX() & 15), (double)(chunkOrigin.getY() & 15), (double)(chunkOrigin.getZ() & 15));
				poseStack.translate(point.x(), point.y(), point.z());
				//poseStack.scale(0.2f, 0.2f, 0.2f);
				poseStack.mulPose(Vector3f.XN.rotationDegrees(90));
				poseStack.translate(-0.5f, -0.5f, -0.5f);
				//Minecraft.getInstance().getBlockRenderer().renderBatched(ModBlocks.I_INSULATOR_BROWN.get().defaultBlockState(), pos, region, poseStack, vertexConsumer, false, new Random());

				//LevelRenderer.renderLineBox(poseStack, vertexConsumer, new AABB(pos), SEGMENTS_AUTO, SEGMENTS_AUTO, SEGMENTS_AUTO, SEGMENTS_AUTO);

				BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
				blockRenderer.getModelRenderer().renderModel(
					poseStack.last(),
					buffers.builder(RenderType.solid()),
					ModBlocks.I_INSULATOR_BROWN.get().defaultBlockState(),
					blockRenderer.getBlockModel(ModBlocks.I_INSULATOR_BROWN.get().defaultBlockState()),
					1,
					1,
					1,
					LevelRenderer.getLightColor(Minecraft.getInstance().level, chunkOrigin),
					0
				);
				poseStack.popPose();
			}
			*/
			//renderCatenary(vertexConsumer, connection.getRelativeStart(), connection.getRelativeEnd());
		}
	}
	
}
