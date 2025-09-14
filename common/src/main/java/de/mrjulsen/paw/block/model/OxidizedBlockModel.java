package de.mrjulsen.paw.block.model;

import java.util.HashMap;
import java.util.Set;

import de.mrjulsen.mcdragonlib.client.model.ModelContext;
import de.mrjulsen.mcdragonlib.client.model.mesh.BasicMesh;
import de.mrjulsen.mcdragonlib.client.model.mesh.Mesh;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock.WeatherState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
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
        Mesh mesh = BasicMesh.fromBakedModel(state, originalModel, random);
        if (state.getBlock() instanceof IWeatheringBlock block) {
            WeatherState ws = (WeatherState)block.getAge();
            HashMap<ResourceLocation, TextureAtlasSprite> oxidizedTextures = new HashMap<>(oxidizingTextures.size());
            for (ResourceLocation texture : oxidizingTextures) {
                oxidizedTextures.computeIfAbsent(texture, tex -> {
                    ResourceLocation newLocation = new ResourceLocation(tex.getNamespace(), tex.getPath() + (ws.getname().isBlank() ? "" : "_" + ws.getname()));
                    return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(newLocation);
                });
            }

            mesh.getFaces().forEach(x -> {
                ResourceLocation location = x.getTexture().contents().name();
                if (oxidizedTextures.containsKey(location)) {
                    x.setTexture(oxidizedTextures.get(location));
                }
            });
        }
        return mesh;
    }    
}
