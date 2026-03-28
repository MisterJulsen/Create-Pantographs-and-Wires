package de.mrjulsen.paw.block.model;

import java.util.HashMap;
import java.util.Set;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.BasicMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.Face;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock.WeatherState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

public class OxidizedBlockModel extends AbstractRotatableBlockModel {

    private final Set<ResourceLocation> oxidizingTextures;

    public OxidizedBlockModel(Set<ResourceLocation> oxidizingTextures) {
        this.oxidizingTextures = oxidizingTextures;
    }

    @Override
    protected Mesh getBaseMesh(ModelType type, BakedModel originalModel, BlockState state, RandomSource random, ModelContext context) {
        Mesh mesh = fromBakedModel(state, originalModel, random);
        if (state.getBlock() instanceof IWeatheringBlock block) {
            WeatherState ws = (WeatherState)block.getAge();
            HashMap<ResourceLocation, TextureAtlasSprite> oxidizedTextures = new HashMap<>(oxidizingTextures.size());
            for (ResourceLocation texture : oxidizingTextures) {
                oxidizedTextures.computeIfAbsent(texture, tex -> {
                    ResourceLocation newLocation = new ResourceLocation(tex.getNamespace(), tex.getPath() + (ws.getName().isBlank() ? "" : "_" + ws.getName()));
                    return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(newLocation);
                });
            }

            mesh.getFaces().forEach(x -> {
                ResourceLocation location = x.getTextureLocation();
                if (oxidizedTextures.containsKey(location)) {
                    x.setTexture(oxidizedTextures.get(location));
                }
            });
        }
        return mesh;
    }
    
    public static BasicMesh fromBakedModel(BlockState state, BakedModel srcModel, RandomSource random) {
        BasicMesh mesh = new BasicMesh();
        Direction[] directions = new Direction[Direction.values().length + 1];
        System.arraycopy(Direction.values(), 0, directions, 0, Direction.values().length);
        for (Direction side : directions) {
            for (BakedQuad quad : srcModel.getQuads(state, side, random)) {
                mesh.addFace(new Face(quad, side));
            }
        }
        return mesh;
    }
}
