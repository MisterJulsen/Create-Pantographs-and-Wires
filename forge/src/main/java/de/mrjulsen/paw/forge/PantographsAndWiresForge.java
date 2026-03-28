package de.mrjulsen.paw.forge;

import com.tterrag.registrate.providers.ProviderType;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.registry.ModBlocks;
import dev.architectury.platform.forge.EventBuses;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PantographsAndWires.MOD_ID)
public class PantographsAndWiresForge {
    public PantographsAndWiresForge() {
        // Submit our event bus to let architectury register our content on the right time
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(PantographsAndWires.MOD_ID, eventBus);
        PantographsAndWires.load();
        PantographsAndWires.REGISTRATE.registerEventListeners(eventBus);
        eventBus.addListener(this::gatherData);
        PantographsAndWires.init();
    }

    private void gatherData(GatherDataEvent event) {
        //PantographsAndWires.REGISTRATE.addDataGenerator(event.getGenerator(), event.getExistingFileHelper().);
    }
}
