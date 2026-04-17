package de.mrjulsen.paw.neoforge;

import de.mrjulsen.paw.PantographsAndWires;
import dev.architectury.platform.hooks.EventBusesHooks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;

@net.neoforged.fml.common.Mod(PantographsAndWires.MOD_ID)
public class PantographsAndWiresNeoForge {
    private static ModContainer modContainer;

    public PantographsAndWiresNeoForge(ModContainer container) {
        modContainer = container;
        PantographsAndWires.load();
        PantographsAndWires.REGISTRATE.registerEventListeners(ModLoadingContext.get().getActiveContainer().getEventBus());
        PantographsAndWires.init();
    }

    static ModContainer getModContainer() {
        return modContainer;
    }
}
