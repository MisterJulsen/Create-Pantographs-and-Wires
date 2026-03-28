package de.mrjulsen.paw.registry;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.util.function.Function;

public enum MastMaterial {
    METAL((weatherState) -> switch (weatherState) {
        case EXPOSED -> DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "block/metal_exposed");
        case WEATHERED -> DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "block/metal_weathered");
        case OXIDIZED -> DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "block/metal_oxidized");
        case GALVANIZED -> DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "block/metal_galvanized");
        default -> DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "block/metal");
    }),
    CONCRETE((weatherState) -> switch (weatherState) {
        case EXPOSED -> DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "block/concrete_post_exposed");
        case WEATHERED -> DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "block/concrete_post_weathered");
        case OXIDIZED -> DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "block/concrete_post_oxidized");
        default -> DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "block/concrete_post");
    });

    final Function<IWeatheringBlock.WeatherState, ResourceLocation> texture;

    MastMaterial(Function<IWeatheringBlock.WeatherState, ResourceLocation> texture) {
        this.texture = texture;
    }

    public ResourceLocation getTexture(IWeatheringBlock.WeatherState weatherState) {
        return this.texture.apply(weatherState);
    }
}
