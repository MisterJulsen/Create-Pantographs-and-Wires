package de.mrjulsen.paw.neoforge.client;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.debug.WireDebugRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, modid = PantographsAndWires.MOD_ID, value = Dist.CLIENT)
public class NeoForgeClientEvents {
    
    @SubscribeEvent
    public static void onRenderLayerPost(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        WireDebugRenderer.renderWireCollisions(event.getPoseStack());
    }
}
