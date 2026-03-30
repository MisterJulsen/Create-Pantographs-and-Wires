package de.mrjulsen.paw.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.DamageTypeTagGen;
import com.tterrag.registrate.util.entry.ItemEntry;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.datagen.DataGen;
import de.mrjulsen.paw.item.FuelItem;
import de.mrjulsen.paw.item.PantographItem;
import de.mrjulsen.wires.item.MultiWireItem;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class ModItems {


    public static final ItemEntry<MultiWireItem> WIRE = PantographsAndWires.REGISTRATE.item("wire_coil", MultiWireItem::new)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();

    public static final ItemEntry<PantographItem> PANTOGRAPH = PantographsAndWires.REGISTRATE.item("pantograph", p -> PantographItem.create(ModBlocks.PANTOGRAPH.get(), p, false))
            .properties(p -> p)
            .model((c, p) -> DataGen.itemModel(c, p, "item/pantograph"))
            .tab(ModCreativeModeTab.MAIN_TAB.getKey())
            .register();

    public static final ItemEntry<FuelItem> COAL_COKE = PantographsAndWires.REGISTRATE.item("coal_coke", FuelItem::new)
            .tab(ModCreativeModeTab.MAIN_TAB.getKey())
            .onRegister(i -> i.setBurnTime(3200))
            .register();

    public static final ItemEntry<Item> CRUSHED_COAL_COKE = PantographsAndWires.REGISTRATE.item("crushed_coal_coke", Item::new)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();
        
    public static final ItemEntry<Item> GRAPHITE_INGOT = PantographsAndWires.REGISTRATE.item("graphite_ingot", Item::new)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();
        
    public static final ItemEntry<Item> IRON_ROD = PantographsAndWires.REGISTRATE.item("iron_rod", Item::new)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();
    
    public static final ItemEntry<Item> IRON_STRIP = PantographsAndWires.REGISTRATE.item("iron_strip", Item::new)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();
    
    public static final ItemEntry<Item> IRON_ROD_BUNDLE = PantographsAndWires.REGISTRATE.item("iron_rod_bundle", Item::new)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();
    
    public static final ItemEntry<Item> PENCIL = PantographsAndWires.REGISTRATE.item("pencil", Item::new)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();

    public static final ItemEntry<Item> COPPER_WIRE = PantographsAndWires.REGISTRATE.item("copper_wire", Item::new)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();

    public static final ItemEntry<Item> EMPTY_WIRE_COIL = PantographsAndWires.REGISTRATE.item("empty_wire_coil", Item::new)
        .tab(ModCreativeModeTab.MAIN_TAB.getKey())
        .register();

    public static final ItemEntry<PantographItem> MOD_ICON = PantographsAndWires.REGISTRATE.item("mod_icon", p -> PantographItem.create(ModBlocks.PANTOGRAPH.get(), p, true))
            .removeTab(CreativeModeTabs.SEARCH)
            .model((c, p) -> DataGen.itemModel(c, p, "item/mod_icon"))
            .register();

    public static final ItemEntry<SequencedAssemblyItem> CANTILEVER_GREEN_INCOMPLETE = PantographsAndWires.REGISTRATE.item("cantilever_incomplete", SequencedAssemblyItem::new)
        .removeTab(CreativeModeTabs.SEARCH)
        .register();

    
    public static void init() {
    }
}
