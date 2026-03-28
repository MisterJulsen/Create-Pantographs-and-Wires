package de.mrjulsen.paw.datagen;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import de.mrjulsen.paw.registry.ModBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class GenTags {

    public static void generateBlockTags(RegistrateTagsProvider<Block> prov) {
        prov.addTag(ModBlocks.TAG_FLAT_LATTICE_MASTS);
    }

    public static void generateItemTags(RegistrateTagsProvider<Item> prov) {
    }

}
