package de.mrjulsen.paw.neoforge.client;

import de.mrjulsen.paw.neoforge.compat.EmbeddiumCompat;
import de.mrjulsen.paw.PantographsAndWires;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = PantographsAndWires.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {
    
    @SubscribeEvent
    public static void registerGeometryLoaders(FMLClientSetupEvent event) {
        if (PantographsAndWires.isEmbeddiumLoaded()) {
            EmbeddiumCompat.register();
        }
    }
}
