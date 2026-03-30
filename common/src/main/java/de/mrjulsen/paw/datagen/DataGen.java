package de.mrjulsen.paw.datagen;

import com.tterrag.registrate.providers.*;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.registry.MastMaterial;
import de.mrjulsen.paw.registry.ModBlockTags;
import de.mrjulsen.paw.registry.ModItemTags;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class DataGen {
    public static void register() {
        PantographsAndWires.REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, ModBlockTags::register);
        PantographsAndWires.REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, ModItemTags::register);
    }

    @ExpectPlatform
    public static <T extends Block> void simpleHorizontalBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String existingModelPath) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> void tensioningDeviceBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String baseModelPath) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> void insulatorBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String baseModelPath, String baseModelName) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> void oxidizingMastBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, MastMaterial material, String basePath, String baseModelName) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> void powerLineBracketBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String basePath) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> void cantileverBracket(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> void cantileverBracketAtPost(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Block> void registrationArm(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String baseModel) {
        throw new AssertionError();
    }



    @ExpectPlatform
    public static void oxidizingItemModel(DataGenContext<Item, BlockItem> context, RegistrateItemModelProvider provider, String prefix, String modelPath) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <E extends ItemLike, R extends E> void itemModel(DataGenContext<E, R> context, RegistrateItemModelProvider provider, String model) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> void registerTags(RegistrateTagsProvider<T> provider, List<TagEntry<T>> registry) {
        throw new AssertionError();
    }
}
