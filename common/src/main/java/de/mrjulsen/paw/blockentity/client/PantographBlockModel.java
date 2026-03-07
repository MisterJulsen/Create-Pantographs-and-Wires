package de.mrjulsen.paw.blockentity.client;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.model.GeoModel;

public class PantographBlockModel extends GeoModel<PantographBlockEntity> {

    public PantographBlockModel() {
        MathParser.setVariable("query.func_start", () -> {
            return PantographBlockEntity.START_ANGLE;
        });
        MathParser.setVariable("query.func2", () -> {
            return PantographBlockEntity.BASE_ANGLE;
        });
        MathParser.setVariable("query.max_height", () -> {
            return PantographBlockEntity.DELTA_HEIGHT_PIXELS;
        });
    }

    @Override
    public ResourceLocation getModelResource(PantographBlockEntity animatable) {
        return DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "geo/block/pantograph.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PantographBlockEntity animatable) {
        return DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "textures/block/pantograph.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PantographBlockEntity animatable) {
        return DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "animations/block/pantograph.animation.json");
    }

    @Override
    public void applyMolangQueries(AnimationState<PantographBlockEntity> animationState, double animTime) {
        super.applyMolangQueries(animationState, animTime);
        animationState.getAnimatable().applyMolangVariables();
    }
}
