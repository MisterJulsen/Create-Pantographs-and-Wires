package de.mrjulsen.paw.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.foundation.data.*;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import de.mrjulsen.paw.CrossPlatform;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.block.CantileverBracketBlock;
import de.mrjulsen.paw.block.CantileverBracketPostConnectionBlock;
import de.mrjulsen.paw.block.CantileverBracketVerticalBlock;
import de.mrjulsen.paw.block.ConcretePillarBlock;
import de.mrjulsen.paw.block.FlatLatticeMastBlock;
import de.mrjulsen.paw.block.HBeamMastBlock;
import de.mrjulsen.paw.block.InsulatorBlock;
import de.mrjulsen.paw.block.LatticeMastBlock;
import de.mrjulsen.paw.block.PantographBlock;
import de.mrjulsen.paw.block.PowerLineBracketBlock;
import de.mrjulsen.paw.block.RegistrationArmBlock;
import de.mrjulsen.paw.block.TensioningDeviceBlock;
import de.mrjulsen.paw.block.UInsulatorBlock;
import de.mrjulsen.paw.block.VInsulatorBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock;
import de.mrjulsen.paw.block.abstractions.IWeatheringBlock.WeatherState;
import de.mrjulsen.paw.block.model.BasicRotatableBlockModel;
import de.mrjulsen.paw.block.model.OxidizedBlockModel;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.blockentity.PantographInteractionBehaviour;
import de.mrjulsen.paw.blockentity.PantographMovementBehaviour;
import de.mrjulsen.paw.client.model.RotatedBlockModel;
import de.mrjulsen.paw.datagen.DataGen;
import de.mrjulsen.paw.item.CantileverBlockItem;
import de.mrjulsen.paw.item.FuelBlockItem;
import de.mrjulsen.paw.item.WaxedBlockItem;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import de.mrjulsen.mcdragonlib.client.model.DLBlockModelRegistry;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;


import static com.simibubi.create.foundation.data.ModelGen.customItemModel;

public class ModBlocks {	

	private static TagKey<Block> createTag(String name) {
		return TagKey.create(Registries.BLOCK, new ResourceLocation(PantographsAndWires.MOD_ID, name));
	}

	public record OxidizingKey(WeatherState weatherState, boolean isWaxed) {}

	private static final int CANTILEVER_CONNECTION_PIXELS = 16;
	private static final List<TagKey<Block>> cantileverConnectableTags = new ArrayList<>();
	static {
		for (int i = 1; i <= CANTILEVER_CONNECTION_PIXELS; i++) {
			cantileverConnectableTags.add(createTag("cantilever_connectable_" + i + "px"));
		}
	}
	public static TagKey<Block> getCantileverConnectableTagFor(int pixels) {
		return cantileverConnectableTags.get(pixels - 1);
	}
	public static int getFirstCantileverConnectionTagForState(BlockState state) {
		for (int i = 0; i < CANTILEVER_CONNECTION_PIXELS; i++) {
			TagKey<Block> tag = cantileverConnectableTags.get(i);
			if (state.getTags().anyMatch(y -> y.equals(tag))) {
				return i + 1;
			}
		}

		return 16;
    }

	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE = createTag("cantilever_connectable");
	public static final TagKey<Block> TAG_CANTILEVER_MAST_BRACKET_FITTING = createTag("cantilever_mast_bracket_fitting");
	public static final TagKey<Block> TAG_CANTILEVER_MAST_HINGE = createTag("cantilever_mast_hinge");
	public static final TagKey<Block> TAG_TENSIONING_DEVICE_CONNECTABLE = createTag("tensioning_device_connectable");

	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_16PX = createTag("cantilever_connectable_16px");
	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_12PX = createTag("cantilever_connectable_12px");
	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_8PX = createTag("cantilever_connectable_8px");
	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_5PX = createTag("cantilever_connectable_5px");
	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_4PX = createTag("cantilever_connectable_4px");

	public static final TagKey<Block> TAG_H_BEAM_MASTS = createTag("h_beam_masts");
	public static final TagKey<Block> TAG_FLAT_LATTICE_MASTS = createTag("flat_lattice_masts");

	public static final TagKey<Block> TAG_CATENARY_HEADSPAN_CONNECTABLE = createTag("catenary_headspan_connectable");
	public static final TagKey<Block> TAG_SUPPORT_WIRE_CONNECTABLE = createTag("support_wire_connectable");

	static {

		PantographsAndWires.REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, p -> {
			p.addTag(TAG_FLAT_LATTICE_MASTS);
		});
	}

	public static final BlockEntry<PantographBlock> PANTOGRAPH = PantographsAndWires.REGISTRATE.block("pantograph", PantographBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.onRegister(MovementBehaviour.movementBehaviour(new PantographMovementBehaviour()))
			.onRegister(MovingInteractionBehaviour.interactionBehaviour(new PantographInteractionBehaviour()))
			.register();
		
	public static final ImmutableMap<OxidizingKey, BlockEntry<LatticeMastBlock>> LATTICE_MAST = registerOxidizingBlock(
			"lattice_mast",
			LatticeMastBlock::new,
			"Lattice Mast",
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/lattice_mast", "base"),
			(weatherState) -> new TagKey[] { ModTags.TAG_LATTICE_MAST, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.item()
					.model((ctx, p) -> DataGen.oxidizingItemModel(ctx, p, "", "block/lattice_mast"))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.build()
	);

	public static final ImmutableMap<OxidizingKey, BlockEntry<FlatLatticeMastBlock>> FLAT_LATTICE_MAST = registerOxidizingBlock(
			"flat_lattice_mast",
			FlatLatticeMastBlock::new,
			"Flat Lattice Mast",
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/flat_lattice_mast", "base"),
			(weatherState) -> new TagKey[] { ModTags.TAG_FLAT_LATTICE_MASTS, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.item()
					.model((ctx, p) -> DataGen.oxidizingItemModel(ctx, p, "", "block/flat_lattice_mast"))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.build()
	);

	public static final ImmutableMap<OxidizingKey, BlockEntry<FlatLatticeMastBlock>> FLAT_LATTICE_MAST_DIAGONAL = registerOxidizingBlock(
			"flat_lattice_mast_diagonal",
			FlatLatticeMastBlock::new,
			"Flat Diagonal Lattice Mast",
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/flat_diagonal_lattice_mast", "base"),
			(weatherState) -> new TagKey[] { ModTags.TAG_FLAT_DIAGONAL_LATTICE_MASTS, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.item()
					.model((ctx, p) -> DataGen.oxidizingItemModel(ctx, p, "", "block/flat_diagonal_lattice_mast"))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.build()
	);		

	public static final ImmutableMap<OxidizingKey, BlockEntry<HBeamMastBlock>> H_BEAM_MAST = registerOxidizingBlock(
			"h_beam_mast",
			HBeamMastBlock::new,
			"H-Beam Mast",
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/h_beam_mast", "base"),
			(weatherState) -> new TagKey[] { ModTags.TAG_H_BEAM_MASTS, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.item()
					.model((ctx, p) -> DataGen.oxidizingItemModel(ctx, p, "", "block/h_beam_mast"))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.build()
	);

	public static final ImmutableMap<OxidizingKey, BlockEntry<CantileverBracketBlock>> CANTILEVER_BRACKET = registerOxidizingBlock(
			"cantilever_bracket",
			CantileverBracketBlock::new,
			"Cantilever Bracket",
			DataGen::cantileverBracket,
			(weatherState) -> new TagKey[] { ModTags.TAG_CONCRETE_PILLARS, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.item()
					.model((ctx, p) -> DataGen.oxidizingItemModel(ctx, p, "base_", "block/cantilever_bracket"))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.build()
	);
	public static final ImmutableMap<OxidizingKey, BlockEntry<CantileverBracketVerticalBlock>> CANTILEVER_BRACKET_VERTICAL = registerOxidizingBlock(
			"cantilever_bracket_vertical",
			CantileverBracketVerticalBlock::new,
			"Vertical Cantilever Bracket",
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/cantilever_bracket_vertical", "base"),
			(weatherState) -> new TagKey[] { ModTags.TAG_CONCRETE_PILLARS, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
	);
	public static final ImmutableMap<OxidizingKey, BlockEntry<CantileverBracketPostConnectionBlock>> CANTILEVER_BRACKET_AT_POST = registerOxidizingBlock(
			"cantilever_bracket_at_post",
			CantileverBracketPostConnectionBlock::new,
			"Cantilever Bracket At Post",
			DataGen::cantileverBracketAtPost,
			(weatherState) -> new TagKey[] { ModTags.TAG_CONCRETE_PILLARS, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
	);

	public static final ImmutableMap<OxidizingKey, BlockEntry<PowerLineBracketBlock>> POWER_LINE_BRACKET = registerOxidizingBlock(
			"power_line_bracket",
			PowerLineBracketBlock::new,
			"Power Line Bracket",
			(ctx, p) -> DataGen.powerLineBracketBlock(ctx, p, "block/power_line_bracket"),
			(weatherState) -> new TagKey[] { ModTags.TAG_CONCRETE_PILLARS, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.item()
					.model((ctx, p) -> DataGen.oxidizingItemModel(ctx, p, "", "block/power_line_bracket/bottom"))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.build()
	);

	public static final BlockEntry<Block> GRAPHITE_BLOCK = PantographsAndWires.REGISTRATE.block("graphite_block", Block::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final ImmutableMap<OxidizingKey, BlockEntry<ConcretePillarBlock>> CONCRETE_POST = registerOxidizingBlock(
			"concrete_post",
			ConcretePillarBlock::post,
			"Concrete Post",
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.CONCRETE, "block/concrete_post", "base"),
			(weatherState) -> new TagKey[] { ModTags.TAG_CONCRETE_MASTS, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			false,
			builder -> builder
					.initialProperties(() -> Blocks.SMOOTH_STONE)
					.transform(TagGen.pickaxeOnly())
					.item()
					.model((ctx, p) -> DataGen.oxidizingItemModel(ctx, p, "", "block/concrete_post"))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.build()
	);

	public static final BlockEntry<Block> COAL_COKE_BLOCK = PantographsAndWires.REGISTRATE.block("coal_coke_block", Block::new)
			.initialProperties(() -> Blocks.DEEPSLATE)
			.transform(TagGen.pickaxeOnly())
			.item(FuelBlockItem::new)
			.onRegister((item) -> item.setBurnTime(32000))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final ImmutableMap<OxidizingKey, BlockEntry<ConcretePillarBlock>> CONCRETE_PILLAR = registerOxidizingBlock(
			"concrete_pillar",
			ConcretePillarBlock::tickPillar,
			"Concrete Pillar",
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.CONCRETE, "block/concrete_pillar", "base"),
			(weatherState) -> new TagKey[] { ModTags.TAG_CONCRETE_PILLARS, ModTags.TAG_CANTILEVER_CONNECTABLE_12PX },
			false,
			builder -> builder
					.initialProperties(() -> Blocks.SMOOTH_STONE)
					.transform(TagGen.pickaxeOnly())
					.item()
					.model((ctx, p) -> DataGen.oxidizingItemModel(ctx, p, "", "block/concrete_pillar"))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.build()
	);

	public static final BlockEntry<TensioningDeviceBlock> TENSIONING_DEVICE = PantographsAndWires.REGISTRATE.block("tensioning_device", TensioningDeviceBlock::new)
			.initialProperties(() -> Blocks.SMOOTH_STONE)
			.transform(TagGen.pickaxeOnly())
			.lang("Tensioning Device")
			.blockstate((c, p) -> DataGen.tensioningDeviceBlock(c, p, "block/tensioning_device"))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.properties(p -> p.stacksTo(16))
			.transform(customItemModel())
			.register();

	public static final BlockEntry<VInsulatorBlock> V_INSULATOR_BROWN = PantographsAndWires.REGISTRATE.block("v_insulator_brown", VInsulatorBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.lang("V-Shaped Insulator")
			.blockstate((c, p) -> DataGen.insulatorBlock(c, p, "block/insulator", "v_insulator_brown"))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.item()
			.model((c, p) -> DataGen.itemModel(c, p, "block/insulator/v_insulator_brown"))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();
	public static final BlockEntry<VInsulatorBlock> V_INSULATOR_GREEN = PantographsAndWires.REGISTRATE.block("v_insulator_green", VInsulatorBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.lang("V-Shaped Insulator")
			.blockstate((c, p) -> DataGen.insulatorBlock(c, p, "block/insulator", "v_insulator_green"))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.addLayer(() -> () -> RenderType.translucent())
			.item()
			.model((c, p) -> DataGen.itemModel(c, p, "block/insulator/v_insulator_green"))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();
		
	public static final BlockEntry<InsulatorBlock> INSULATOR_BROWN = PantographsAndWires.REGISTRATE.block("insulator_brown", InsulatorBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.lang("Insulator")
			.blockstate((c, p) -> DataGen.insulatorBlock(c, p, "block/insulator", "i_insulator_brown"))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.item()
			.model((c, p) -> DataGen.itemModel(c, p, "block/insulator/base/brown_insulator"))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();
	public static final BlockEntry<InsulatorBlock> INSULATOR_GREEN = PantographsAndWires.REGISTRATE.block("insulator_green", InsulatorBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.lang("Insulator")
			.blockstate((c, p) -> DataGen.insulatorBlock(c, p, "block/insulator", "i_insulator_green"))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.addLayer(() -> () -> RenderType.translucent())
			.item()
			.model((c, p) -> DataGen.itemModel(c, p, "block/insulator/base/green_insulator"))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final BlockEntry<UInsulatorBlock> U_INSULATOR_GREEN = PantographsAndWires.REGISTRATE.block("u_insulator_green", UInsulatorBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.lang("U-Shaped Insulator")
			.blockstate((c, p) -> DataGen.insulatorBlock(c, p, "block/insulator", "u_insulator_green"))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.addLayer(() -> () -> RenderType.translucent())
			.item()
			.model((c, p) -> DataGen.itemModel(c, p, "block/insulator/u_insulator_green"))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();
	public static final BlockEntry<UInsulatorBlock> U_INSULATOR_BROWN = PantographsAndWires.REGISTRATE.block("u_insulator_brown", UInsulatorBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.lang("U-Shaped Insulator")
			.blockstate((c, p) -> DataGen.insulatorBlock(c, p, "block/insulator", "u_insulator_brown"))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.item()
			.model((c, p) -> DataGen.itemModel(c, p, "block/insulator/u_insulator_brown"))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final BlockEntry<RegistrationArmBlock> REGISTRATION_ARM = PantographsAndWires.REGISTRATE.block("registration_arm", RegistrationArmBlock::new)
			.blockstate((ctx, p) -> DataGen.registrationArm(ctx, p, "block/registration_arm"))
			.register();
		
    public static void init() {
		registerCantilevers();
	}

	public static final Map<EInsulatorType, BlockEntry<? extends AbstractCantileverBlock>> CANTILEVERS = new HashMap<>();
	public static final Collection<NonNullSupplier<? extends Block>> CANTILEVER_BLOCK_ENTITY_BLOCKS = new ArrayList<>();
	public static final Map<EInsulatorType, ItemEntry<CantileverBlockItem<CantileverBlock>>> CANTILEVER_ITEMS = new HashMap<>();

	public static BlockEntry<? extends AbstractCantileverBlock> getCantilever(EInsulatorType type) {
		return CANTILEVERS.get(type);
	}

	public static Collection<BlockEntry<? extends AbstractCantileverBlock>> getCantilevers() {
		return CANTILEVERS.values();
	}

	private static void registerCantilevers() {
		for (EInsulatorType type : EInsulatorType.values()) {
			final EInsulatorType t = type;
			BlockEntry<CantileverBlock> cantileverBlock = registerBlockEntityBlock(CANTILEVER_BLOCK_ENTITY_BLOCKS, PantographsAndWires.REGISTRATE.block(String.format("cantilever_%s", type.getSerializedName()), p -> new CantileverBlock(p, t))
				.initialProperties(SharedProperties::softMetal)
				.transform(TagGen.pickaxeOnly())
				.addLayer(() -> () -> RenderType.translucent())
				.register()
			);
			CANTILEVERS.put(t, cantileverBlock);

			DLUtils.doIfNotNull(cantileverBlock, x -> {	
				final BlockEntry<CantileverBlock> y = x;
				ItemEntry<CantileverBlockItem<CantileverBlock>> item = PantographsAndWires.REGISTRATE.item(String.format("cantilever_%s", type.getSerializedName()), p -> new CantileverBlockItem<>(y.get(), type, p))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.register()
				;
				CANTILEVER_ITEMS.put(type, item);
			});
		}
	}

	@FunctionalInterface
	private static interface IOxidizingBlockFactory<T extends Block & IWeatheringBlock<T>> {
		T create(Properties properties, IWeatheringBlock.WeatheringData<T> weatheringData);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Block & IWeatheringBlock<T>> ImmutableMap<OxidizingKey, BlockEntry<T>> registerOxidizingBlock(
			String baseId,
			IOxidizingBlockFactory<T> factory,
			String baseName,
			NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> blockStateGen,
			Function<WeatherState, TagKey<Block>[]> tagsFactory,
			boolean addGalvanizedState,
			UnaryOperator<BlockBuilder<T, CreateRegistrate>> commonBuilder
	) {
		AtomicReference<BlockEntry<T>> previous = new AtomicReference<>(null);
		Map<OxidizingKey, BlockEntry<T>> variants = new HashMap<>(WeatherState.values().length);
		boolean[] waxedState = { false, true };
		for (boolean waxed : waxedState) {
			previous.set(null);
			for (int i = WeatherState.values().length - 1; i >= 0; i--) {
				final WeatherState s = WeatherState.values()[i];
				if (s == WeatherState.GALVANIZED && (!addGalvanizedState || waxed)) {
					continue;
				}

				final BlockEntry<T> prev = previous.get();
				final boolean isWaxed = waxed;

				String id = (isWaxed ? "waxed_" : "") + baseId + (!s.getName().isBlank() ? "_" + s.getName() : "");

				TagKey[] tags = {
						(switch (s) {
							case EXPOSED -> ModTags.TAG_EXPOSED_MASTS;
							case WEATHERED -> ModTags.TAG_WEATHERED_MASTS;
							case OXIDIZED -> ModTags.TAG_OXIDIZED_MASTS;
							case GALVANIZED -> ModTags.TAG_GALVANIZED_MASTS;
							default -> ModTags.TAG_RAW_MASTS;
						}),
						(isWaxed ? ModTags.TAG_WAXED_MASTS : ModTags.TAG_UNWAXED_MASTS)
				};

				BlockEntry<T> block = commonBuilder.apply(PantographsAndWires.REGISTRATE.block(id, p -> factory.create(p, new IWeatheringBlock.WeatheringData<>(s, () -> prev == null ? null : prev.get(), isWaxed))))
						.lang((isWaxed ? "Waxed " : "") + s.getName() + baseName)
						.blockstate(blockStateGen)
						.tag(tags)
						.tag(tagsFactory.apply(s))
						.register();

				if (Platform.getEnvironment() == Env.CLIENT) {
					DLBlockModelRegistry.registerForBlock(block::get, BasicRotatableBlockModel::new, BasicRotatableBlockModel::new);
				}
				variants.put(new OxidizingKey(s, waxed), block);
				previous.set(s == WeatherState.GALVANIZED ? null : block);
			}
		}
		return ImmutableMap.copyOf(variants);
	}

	private static <T extends BlockEntry<? extends Block>> T registerBlockEntityBlock(Collection<NonNullSupplier<? extends Block>> mem, T e) {
		mem.add(e);
		return e;
	}
}
