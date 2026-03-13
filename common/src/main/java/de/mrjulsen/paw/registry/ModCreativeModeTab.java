package de.mrjulsen.paw.registry;

import com.tterrag.registrate.util.entry.RegistryEntry;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.mcdragonlib.util.TextUtils;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static de.mrjulsen.paw.registry.ModItems.PANTOGRAPH;

public class ModCreativeModeTab {

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> MAIN_TAB = PantographsAndWires.REGISTRATE.defaultCreativeTab("main", b -> b
            .icon(() -> new ItemStack(ModItems.MOD_ICON.get()))
            .title(TextUtils.text("Create: Pantographs & Wires"))
            .build()
    ).register();
    
    public static void setup() { }
}