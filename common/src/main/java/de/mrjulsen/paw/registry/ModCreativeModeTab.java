package de.mrjulsen.paw.registry;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeModeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(PantographsAndWires.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
        "tab", // Tab ID
        () -> CreativeTabRegistry.create(
                TextUtils.translate("itemGroup." + PantographsAndWires.MOD_ID + ".tab"), // Tab Name
                () -> new ItemStack(ModItems.MOD_ICON.get()) // Icon
        )
    );
    
    public static void setup() {
        CREATIVE_MODE_TABS.register();
    }
}