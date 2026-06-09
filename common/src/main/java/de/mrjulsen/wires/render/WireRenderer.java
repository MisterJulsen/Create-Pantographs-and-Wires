package de.mrjulsen.wires.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import de.mrjulsen.mcdragonlib.util.Cache;
<<<<<<< HEAD
import de.mrjulsen.paw.mixin.client.RenderChunkAccess;
=======
import de.mrjulsen.mcdragonlib.util.DLUtils;
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
import de.mrjulsen.wires.WiresApi;
import de.mrjulsen.wires.graph.WireGraphClient;
import de.mrjulsen.wires.graph.WireGraphManager;
import de.mrjulsen.wires.util.ClientUtils;
import de.mrjulsen.wires.util.CompiledChunkExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import java.util.Collection;
import java.util.function.Function;

import javax.annotation.Nonnull;

public class WireRenderer implements ResourceManagerReloadListener {

	public static final Cache<TextureAtlasSprite> WIRE_TEXTURE = new Cache<>(
			() -> Minecraft.getInstance().getModelManager()
					.getAtlas(InventoryMenu.BLOCK_ATLAS)
					.getSprite(DLUtils.resourceLocation(WiresApi.MOD_ID, "block/wire")));

	@Override
	public void onResourceManagerReload(@Nonnull ResourceManager pResourceManager) {
		WIRE_TEXTURE.clear();
	}

	public static void renderConnectionsInSection(Function<RenderType, VertexConsumer> buffers, BlockAndTintGetter region, SectionPos section) {
		for (WireGraphClient graph : WireGraphManager.getAllClient(ClientUtils.level())) {
			if (!graph.hasConnectionsInSection(section)) continue;

			RenderType renderType = RenderType.cutout();
			VertexConsumer builder = buffers.apply(renderType);

			Collection<WireSegmentRenderDataBatch> connections = graph.connectionsInSection(section);
			for (WireSegmentRenderDataBatch connection : connections) {
				connection.render(region, builder);
			}
		}
	}
}
