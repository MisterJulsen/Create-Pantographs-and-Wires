package de.mrjulsen.paw.neoforge;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.registry.ModBlockEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = PantographsAndWires.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NeoForgeModEvents {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.WIRE_CONNECTOR_BLOCK_ENTITY.get(),
            (be, side) -> be
        );
    }
}
