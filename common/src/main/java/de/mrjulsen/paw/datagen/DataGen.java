package de.mrjulsen.paw.datagen;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.LatticeMastBlock;
import de.mrjulsen.paw.block.RegistrationArmBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.registry.MastMaterial;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.fabricators_of_create.porting_lib.models.generators.ConfiguredModel;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class DataGen {
    public static void register() {
        PantographsAndWires.REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, GenTags::generateBlockTags);
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
}
