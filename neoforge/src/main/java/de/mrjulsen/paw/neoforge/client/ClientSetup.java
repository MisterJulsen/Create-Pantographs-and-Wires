package de.mrjulsen.paw.neoforge.client;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.wires.render.WireRenderer;
import net.minecraft.core.SectionPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = PantographsAndWires.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onSectionRender(AddSectionGeometryEvent event) {
        event.addRenderer(e -> WireRenderer.renderConnectionsInSection(e::getOrCreateChunkBuffer, e.getRegion(), SectionPos.of(event.getSectionOrigin())));
    }
}
