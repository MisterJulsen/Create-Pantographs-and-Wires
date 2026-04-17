package de.mrjulsen.paw.registry;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.datagen.DataGen;
import de.mrjulsen.paw.datagen.ITagAppender;
import de.mrjulsen.paw.datagen.TagEntry;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public final class ModItemTags {
    private ModItemTags() {}

    private static final List<TagEntry<Item>> REGISTRY = new LinkedList<>();

    private static TagKey<Item> tag(String name) {
        return tag(name, $ -> {});
    }

    private static TagKey<Item> tag(String name, Consumer<ITagAppender<Item>> populator) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, DLUtils.resourceLocation(PantographsAndWires.MOD_ID, name));
        REGISTRY.add(new TagEntry<>(key, populator));
        return key;
    }


    public static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, DLUtils.resourceLocation("c:tools/wrench"));
    public static final TagKey<Item> CANTILEVERS = tag("cantilevers");
    public static final TagKey<Item> INSULATORS = tag("insulators");


    public static void register(RegistrateTagsProvider<Item> provider) {
        if (Platform.isFabric()) {
            DataGen.registerTags(provider, REGISTRY);
        }
    }
}
