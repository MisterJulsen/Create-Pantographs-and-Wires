package de.mrjulsen.paw.forge.client;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.debug.WireDebugRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(bus = Bus.FORGE, modid = PantographsAndWires.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEvents {
    
    @SubscribeEvent
    public static void onRenderLayerPost(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        WireDebugRenderer.renderWireCollisions(event.getPoseStack());
    }
}
