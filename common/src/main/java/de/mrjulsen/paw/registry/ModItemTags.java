package de.mrjulsen.paw.registry;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
<<<<<<< HEAD
=======
import de.mrjulsen.mcdragonlib.util.DLUtils;
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
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
<<<<<<< HEAD
        TagKey<Item> key = TagKey.create(Registries.ITEM, new ResourceLocation(PantographsAndWires.MOD_ID, name));
=======
        TagKey<Item> key = TagKey.create(Registries.ITEM, DLUtils.resourceLocation(PantographsAndWires.MOD_ID, name));
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
        REGISTRY.add(new TagEntry<>(key, populator));
        return key;
    }


<<<<<<< HEAD
    public static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, new ResourceLocation(Platform.isFabric() ? "c:wrenches" : "forge:tools/wrench"));
=======
    public static final TagKey<Item> WRENCHES = TagKey.create(Registries.ITEM, DLUtils.resourceLocation("c:tools/wrench"));
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
    public static final TagKey<Item> CANTILEVERS = tag("cantilevers");
    public static final TagKey<Item> INSULATORS = tag("insulators");


    public static void register(RegistrateTagsProvider<Item> provider) {
        if (Platform.isFabric()) {
            DataGen.registerTags(provider, REGISTRY);
        }
    }
}
