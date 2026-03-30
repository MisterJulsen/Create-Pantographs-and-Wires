package de.mrjulsen.paw.datagen.fabric;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.*;
import de.mrjulsen.paw.block.abstractions.AbstractMultipartPostBlock;
import de.mrjulsen.paw.block.abstractions.IHorizontalExtensionConnectable;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.property.ECantileverConnectionType;
import de.mrjulsen.paw.block.property.EPostPart;
import de.mrjulsen.paw.datagen.ITagAppender;
import de.mrjulsen.paw.datagen.TagEntry;
import de.mrjulsen.paw.registry.MastMaterial;
import io.github.fabricators_of_create.porting_lib.models.generators.ConfiguredModel;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Half;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class DataGenImpl {


    public static <T> void registerTags(RegistrateTagsProvider<T> provider, List<TagEntry<T>> registry) {
        for (TagEntry<T> entry : registry) {
            ITagAppender<T> appender = new TagAppender<>(provider.addTag(entry.key()));
            entry.populator().accept(appender);
        }
    }

    public static <T extends Block> void simpleHorizontalBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String existingModelPath) {
        provider.horizontalBlock(
                context.getEntry(),
                provider
                        .models()
                        .getExistingFile(
                                provider.modLoc(existingModelPath)
                        )
        );
    }

    public static <E extends ItemLike, R extends E> void itemModel(DataGenContext<E, R> context, RegistrateItemModelProvider provider, String model) {
        provider.getBuilder(context.getName()).parent(new ModelFile.UncheckedModelFile(provider.modLoc(model)));
    }


    public static <T extends Block> void insulatorBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String baseModelPath, String baseModelName) {
        provider.horizontalBlock(context.getEntry(), state -> {
            String half = "";
            if (state.getBlock() instanceof InsulatorBlock) {
                half = "_" + switch (state.getValue(InsulatorBlock.HALF)) {
                    case TOP -> "floor";
                    default -> "ceiling";
                };
            }
            String modelPath = baseModelPath + "/" + baseModelName + half;
            return provider
                    .models()
                    .getExistingFile(provider.modLoc(modelPath))
            ;
        });
    }

    public static void oxidizingItemModel(DataGenContext<Item, BlockItem> context, RegistrateItemModelProvider provider, String prefix, String modelPath) {
        Block block = context.getEntry().getBlock();
        String suffix = "normal";
        if (block instanceof IWeatheringBlock<?> weatheringBlock) {
            IWeatheringBlock.WeatherState state = weatheringBlock.getWeatheringData().weatherState();
            suffix = state == IWeatheringBlock.WeatherState.UNAFFECTED ? suffix : state.getName();
        }
        String parentPath = modelPath + "/" + prefix + suffix;
        provider.getBuilder(context.getName()).parent(new ModelFile.UncheckedModelFile(provider.modLoc(parentPath)));
    }

    public static <T extends Block> void oxidizingMastBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, MastMaterial material, String basePath, String baseModelName) {
        T block = context.getEntry();

        IWeatheringBlock.WeatherState weatherState = IWeatheringBlock.WeatherState.UNAFFECTED;
        if (block instanceof IWeatheringBlock<?> weatheringBlock) {
            weatherState = weatheringBlock.getWeatheringData().weatherState();
        }
        final IWeatheringBlock.WeatherState fWeatherState = weatherState;

        provider.horizontalBlock(context.getEntry(), state -> {
            EPostPart part = EPostPart.IN_BETWEEN;
            if (state.getBlock() instanceof AbstractMultipartPostBlock) {
                part = state.getValue(AbstractMultipartPostBlock.PART);
            }

            String modelPath = basePath + "/" +
                    (fWeatherState != IWeatheringBlock.WeatherState.UNAFFECTED ? fWeatherState.getName() : "normal") +
                    (part != EPostPart.IN_BETWEEN ? "_" + part.getName() : "")
            ;

            return provider
                    .models()
                    .withExistingParent(modelPath, DLUtils.resourceLocation(PantographsAndWires.MOD_ID, basePath + "/" + baseModelName + (part != EPostPart.IN_BETWEEN ? "_" + part.getName() : "")))
                    .texture("base", material.getTexture(fWeatherState))
                    .texture("particle", material.getTexture(fWeatherState))
                    ;
        });
    }

    public static <T extends Block> void tensioningDeviceBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String baseModelPath) {
        provider.horizontalBlock(context.getEntry(), state -> {
            ECantileverConnectionType connection = ECantileverConnectionType.PX16;
            if (state.getBlock() instanceof TensioningDeviceBlock) {
                connection = state.getValue(TensioningDeviceBlock.CONNECTION);
            }

            String modelPath = baseModelPath + "/" + connection.getName();

            return provider
                    .models()
                    .getExistingFile(DLUtils.resourceLocation(PantographsAndWires.MOD_ID, modelPath))
                    ;
        });
    }

    public static <T extends Block> void powerLineBracketBlock(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String basePath) {
        T block = context.getEntry();

        IWeatheringBlock.WeatherState weatherState = IWeatheringBlock.WeatherState.UNAFFECTED;
        if (block instanceof IWeatheringBlock<?> weatheringBlock) {
            weatherState = weatheringBlock.getWeatheringData().weatherState();
        }
        final IWeatheringBlock.WeatherState fWeatherState = weatherState;

        provider.getVariantBuilder(block).forAllStates(state -> {
            Half half = state.getValue(PowerLineBracketBlock.HALF);
            IHorizontalExtensionConnectable.EPostType postType = state.getValue(PowerLineBracketBlock.POST_TYPE);
            PowerLineBracketBlock.EConnectionType connectionType = state.getValue(PowerLineBracketBlock.CONNECTION_TYPE);
            Direction facing = state.getValue(PowerLineBracketBlock.FACING);

            String modelName = switch (connectionType) {
                case ON_POST -> switch (postType) {
                    case LATTICE -> "on_lattice";
                    case FENCE -> "on_fence";
                    default -> "on_wall";
                };
                case ON_POST_EXTENSION -> switch (postType) {
                    case LATTICE -> "on_lattice2";
                    case FENCE -> "on_fence2";
                    default -> "on_wall2";
                };
                case AT_POST -> switch (postType) {
                    case LATTICE -> "at_lattice";
                    case FENCE -> "at_fence";
                    default -> "at_wall";
                };
                default -> "";
            };


            String halfPath = half == Half.BOTTOM ? "bottom" : "top";
            String s = modelName.isBlank() ? "" : "_" + modelName;
            String baseModel = basePath + "/" + halfPath + "/" + "power_line_bracket" + s;

            modelName += (modelName.isBlank() ? "" : "_") + (fWeatherState == IWeatheringBlock.WeatherState.UNAFFECTED ? "normal" : fWeatherState.getName());
            String fullModelPath = basePath + "/" + halfPath + "/" + modelName;

            int yRot = (int)facing.toYRot() + 180;

            ModelFile model = provider.models()
                    .withExistingParent(fullModelPath, DLUtils.resourceLocation(PantographsAndWires.MOD_ID, baseModel))
                    .texture("base", MastMaterial.METAL.getTexture(fWeatherState))
                    .texture("particle", MastMaterial.METAL.getTexture(fWeatherState))
            ;

            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(yRot)
                    .build();
        });
    }

    public static <T extends Block> void registrationArm(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider, String baseModel) {
        T block = context.getEntry();
        provider.getVariantBuilder(block).forAllStates(state -> {
            RegistrationArmBlock.State armState = state.getValue(RegistrationArmBlock.REGISTRATION_ARM);
            boolean isMirrored = state.getValue(RegistrationArmBlock.MIRRORED);
            return ConfiguredModel.builder()
                    .modelFile(provider.models().getExistingFile(provider.modLoc(baseModel + (armState == RegistrationArmBlock.State.ABOVE ? "_above" : ""))))
                    .rotationY(isMirrored ? 180 : 0)
                    .build();
        });
    }

    public static <T extends Block> void cantileverBracket(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider) {
        T block = context.getEntry();

        Function<String, ModelFile> model = getCantileverBracketModelFile(provider, block, "");

        provider.getMultipartBuilder(block)
                // Base
                .part().modelFile(model.apply("base"))
                .addModel().condition(CantileverBracketBlock.FACING, Direction.NORTH).end()
                .part().modelFile(model.apply("base"))
                .rotationY(90).addModel().condition(CantileverBracketBlock.FACING, Direction.EAST).end()
                .part().modelFile(model.apply("base"))
                .rotationY(180).addModel().condition(CantileverBracketBlock.FACING, Direction.SOUTH).end()
                .part().modelFile(model.apply("base"))
                .rotationY(270).addModel().condition(CantileverBracketBlock.FACING, Direction.WEST).end()

                // Crossing post
                .part().modelFile(model.apply("crossing_post_part"))
                .addModel().condition(CantileverBracketBlock.UP, true).end()
                .part().modelFile(model.apply("crossing_post_part"))
                .rotationX(180).addModel().condition(CantileverBracketBlock.DOWN, true).end()

                // T-Cross (down)
                .part().modelFile(model.apply("t_cross"))
                .addModel()
                .condition(CantileverBracketBlock.FACING, Direction.NORTH)
                .condition(CantileverBracketBlock.UP, false)
                .condition(CantileverBracketBlock.DOWN, true).end()
                .part().modelFile(model.apply("t_cross"))
                .rotationY(90).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.EAST)
                .condition(CantileverBracketBlock.UP, false)
                .condition(CantileverBracketBlock.DOWN, true).end()
                .part().modelFile(model.apply("t_cross"))
                .rotationY(180).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.SOUTH)
                .condition(CantileverBracketBlock.UP, false)
                .condition(CantileverBracketBlock.DOWN, true).end()
                .part().modelFile(model.apply("t_cross"))
                .rotationY(270).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.WEST)
                .condition(CantileverBracketBlock.UP, false)
                .condition(CantileverBracketBlock.DOWN, true).end()

                // T-Cross (up)
                .part().modelFile(model.apply("t_cross"))
                .rotationX(180).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.NORTH)
                .condition(CantileverBracketBlock.UP, true)
                .condition(CantileverBracketBlock.DOWN, false).end()
                .part().modelFile(model.apply("t_cross"))
                .rotationY(90).rotationX(180).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.EAST)
                .condition(CantileverBracketBlock.UP, true)
                .condition(CantileverBracketBlock.DOWN, false).end()
                .part().modelFile(model.apply("t_cross"))
                .rotationY(180).rotationX(180).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.SOUTH)
                .condition(CantileverBracketBlock.UP, true)
                .condition(CantileverBracketBlock.DOWN, false).end()
                .part().modelFile(model.apply("t_cross"))
                .rotationY(270).rotationX(180).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.WEST)
                .condition(CantileverBracketBlock.UP, true)
                .condition(CantileverBracketBlock.DOWN, false).end()

                // Cross
                .part().modelFile(model.apply("cross"))
                .addModel()
                .condition(CantileverBracketBlock.FACING, Direction.NORTH)
                .condition(CantileverBracketBlock.UP, true)
                .condition(CantileverBracketBlock.DOWN, true).end()
                .part().modelFile(model.apply("cross"))
                .rotationY(90).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.EAST)
                .condition(CantileverBracketBlock.UP, true)
                .condition(CantileverBracketBlock.DOWN, true).end()
                .part().modelFile(model.apply("cross"))
                .rotationY(180).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.SOUTH)
                .condition(CantileverBracketBlock.UP, true)
                .condition(CantileverBracketBlock.DOWN, true).end()
                .part().modelFile(model.apply("cross"))
                .rotationY(270).addModel()
                .condition(CantileverBracketBlock.FACING, Direction.WEST)
                .condition(CantileverBracketBlock.UP, true)
                .condition(CantileverBracketBlock.DOWN, true).end();
    }

    public static <T extends Block> void cantileverBracketAtPost(DataGenContext<Block, T> context, RegistrateBlockstateProvider provider) {
        T block = context.getEntry();

        Function<String, ModelFile> model = getCantileverBracketModelFile(provider, block, "_at_post");

        provider.getMultipartBuilder(block)
                // Base
                .part().modelFile(model.apply("base"))
                .addModel().condition(CantileverBracketBlock.FACING, Direction.NORTH).end()
                .part().modelFile(model.apply("base"))
                .rotationY(90).addModel().condition(CantileverBracketBlock.FACING, Direction.EAST).end()
                .part().modelFile(model.apply("base"))
                .rotationY(180).addModel().condition(CantileverBracketBlock.FACING, Direction.SOUTH).end()
                .part().modelFile(model.apply("base"))
                .rotationY(270).addModel().condition(CantileverBracketBlock.FACING, Direction.WEST).end()

                // Bracket
                .part().modelFile(model.apply("bracket"))
                .addModel().condition(CantileverBracketBlock.FACING, Direction.NORTH).end()
                .part().modelFile(model.apply("bracket"))
                .rotationY(90).addModel().condition(CantileverBracketBlock.FACING, Direction.EAST).end()
                .part().modelFile(model.apply("bracket"))
                .rotationY(180).addModel().condition(CantileverBracketBlock.FACING, Direction.SOUTH).end()
                .part().modelFile(model.apply("bracket"))
                .rotationY(270).addModel().condition(CantileverBracketBlock.FACING, Direction.WEST).end();
    }


    private static <T extends Block> @NotNull Function<String, ModelFile> getCantileverBracketModelFile(RegistrateBlockstateProvider provider, T block, String suffix) {
        final String basePath = "block/cantilever_bracket";

        IWeatheringBlock.WeatherState weatherState = IWeatheringBlock.WeatherState.UNAFFECTED;
        if (block instanceof IWeatheringBlock<?> weatheringBlock) {
            weatherState = weatheringBlock.getWeatheringData().weatherState();
        }
        final IWeatheringBlock.WeatherState fWeatherState = weatherState;

        Function<String, ModelFile> model = (o) -> {
            String newName = o + suffix;
            return provider.models()
                    .withExistingParent(basePath + "/" + newName + "_" + (fWeatherState == IWeatheringBlock.WeatherState.UNAFFECTED ? "normal" : fWeatherState.getName()), provider.modLoc(basePath + "/" + o))
                    .texture("base", MastMaterial.METAL.getTexture(fWeatherState))
                    .texture("particle", MastMaterial.METAL.getTexture(fWeatherState))
            ;
        };
        return model;
    }
}
