package de.mrjulsen.paw.registry;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.data.*;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

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
import de.mrjulsen.paw.block.abstractions.weathering.IAgingBlock;
import de.mrjulsen.paw.block.model.BasicRotatableBlockModel;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.blockentity.PantographInteractionBehaviour;
import de.mrjulsen.paw.blockentity.PantographMovementBehaviour;
import de.mrjulsen.paw.client.model.RotatedBlockModel;
import de.mrjulsen.paw.datagen.DataGen;
import de.mrjulsen.paw.item.CantileverBlockItem;
import de.mrjulsen.paw.item.FuelBlockItem;
import de.mrjulsen.paw.util.Utils;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import de.mrjulsen.mcdragonlib.client.model.DLBlockModelRegistry;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.Nullable;


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

	public static final BlockEntry<PantographBlock> PANTOGRAPH = PantographsAndWires.REGISTRATE.block("pantograph", PantographBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.lang("Pantograph")
			.blockstate((c, p) -> DataGen.simpleHorizontalBlock(c, p, "block/pantograph"))
			.transform(TagGen.pickaxeOnly())
			.onRegister(MovementBehaviour.movementBehaviour(new PantographMovementBehaviour()))
			.onRegister(MovingInteractionBehaviour.interactionBehaviour(new PantographInteractionBehaviour()))
			.register();

	public static final ImmutableMap<OxidizingKey, BlockEntry<LatticeMastBlock>> LATTICE_MAST = registerOxidizingBlock(
			"lattice_mast",
			LatticeMastBlock::new,
			"Lattice Mast",
			MastMaterial.METAL,
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/lattice_mast", "base"),
			(weatherState) -> new TagKey[] {
					ModBlockTags.LATTICE_MASTS,
					ModBlockTags.CANTILEVER_CONNECTABLE_12PX,
					ModBlockTags.TENSIONING_DEVICE_CONNECTABLE,
					ModBlockTags.CATENARY_HEADSPAN_CONNECTABLE,
					ModBlockTags.SUPPORT_WIRE_CONNECTABLE,
					ModBlockTags.CANTILEVER_MAST_BRACKET_FITTING
			},
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
			MastMaterial.METAL,
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/flat_lattice_mast", "base"),
			(weatherState) -> new TagKey[] { ModBlockTags.FLAT_LATTICE_MASTS, ModBlockTags.CANTILEVER_CONNECTABLE_8PX, ModBlockTags.SUPPORT_WIRE_CONNECTABLE },
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
			MastMaterial.METAL,
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/flat_diagonal_lattice_mast", "base"),
			(weatherState) -> new TagKey[] { ModBlockTags.FLAT_DIAGONAL_LATTICE_MASTS, ModBlockTags.CANTILEVER_CONNECTABLE_8PX, ModBlockTags.SUPPORT_WIRE_CONNECTABLE },
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
			MastMaterial.METAL,
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/h_beam_mast", "base"),
			(weatherState) -> new TagKey[] {
					ModBlockTags.H_BEAM_MASTS,
					ModBlockTags.CANTILEVER_CONNECTABLE_6PX,
					ModBlockTags.SUPPORT_WIRE_CONNECTABLE,
					ModBlockTags.CANTILEVER_MAST_HINGE
			},
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
			MastMaterial.METAL,
			DataGen::cantileverBracket,
			(weatherState) -> new TagKey[] { ModBlockTags.CANTILEVER_BRACKETS, ModBlockTags.SUPPORT_WIRE_CONNECTABLE },
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
			MastMaterial.METAL,
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.METAL, "block/cantilever_bracket_vertical", "base"),
			(weatherState) -> new TagKey[] { ModBlockTags.CANTILEVER_BRACKETS, ModBlockTags.CANTILEVER_CONNECTABLE_4PX, ModBlockTags.SUPPORT_WIRE_CONNECTABLE },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.loot((lt, block) -> lt.dropOther(block, Objects.requireNonNull(CANTILEVER_BRACKET.get(new OxidizingKey(block.getWeatheringData().ageState(), block.getWeatheringData().isWaxed()))).get()))
	);
	public static final ImmutableMap<OxidizingKey, BlockEntry<CantileverBracketPostConnectionBlock>> CANTILEVER_BRACKET_AT_POST = registerOxidizingBlock(
			"cantilever_bracket_at_post",
			CantileverBracketPostConnectionBlock::new,
			"Cantilever Bracket (At Post)",
			MastMaterial.METAL,
			DataGen::cantileverBracketAtPost,
			(weatherState) -> new TagKey[] { ModBlockTags.CANTILEVER_BRACKETS, ModBlockTags.SUPPORT_WIRE_CONNECTABLE },
			true,
			builder -> builder
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.loot((lt, block) -> lt.dropOther(block, Objects.requireNonNull(CANTILEVER_BRACKET.get(new OxidizingKey(block.getWeatheringData().ageState(), block.getWeatheringData().isWaxed()))).get()))
	);

	public static final ImmutableMap<OxidizingKey, BlockEntry<PowerLineBracketBlock>> POWER_LINE_BRACKET = registerOxidizingBlock(
			"power_line_bracket",
			PowerLineBracketBlock::new,
			"Power Line Bracket",
			MastMaterial.METAL,
			(ctx, p) -> DataGen.powerLineBracketBlock(ctx, p, "block/power_line_bracket"),
			(weatherState) -> new TagKey[] { ModBlockTags.POWER_LINE_BRACKETS },
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
			.lang("Graphite Block")
			.item()
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final ImmutableMap<OxidizingKey, BlockEntry<ConcretePillarBlock>> CONCRETE_POST = registerOxidizingBlock(
			"concrete_post",
			ConcretePillarBlock::post,
			"Concrete Post",
			MastMaterial.CONCRETE,
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.CONCRETE, "block/concrete_post", "base"),
			(weatherState) -> new TagKey[] {
					ModBlockTags.CONCRETE_POSTS,
					ModBlockTags.CANTILEVER_CONNECTABLE_5PX,
					ModBlockTags.SUPPORT_WIRE_CONNECTABLE,
					ModBlockTags.CANTILEVER_MAST_HINGE
			},
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
			.lang("Coal Coke Block")
			.item(FuelBlockItem::new)
			.onRegister((item) -> item.setBurnTime(32000))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.build()
			.register();

	public static final ImmutableMap<OxidizingKey, BlockEntry<ConcretePillarBlock>> CONCRETE_PILLAR = registerOxidizingBlock(
			"concrete_pillar",
			ConcretePillarBlock::tickPillar,
			"Concrete Pillar",
			MastMaterial.CONCRETE,
			(ctx, p) -> DataGen.oxidizingMastBlock(ctx, p, MastMaterial.CONCRETE, "block/concrete_pillar", "base"),
			(weatherState) -> new TagKey[] {
					ModBlockTags.CONCRETE_PILLARS,
					ModBlockTags.CANTILEVER_CONNECTABLE_8PX,
					ModBlockTags.TENSIONING_DEVICE_CONNECTABLE,
					ModBlockTags.CATENARY_HEADSPAN_CONNECTABLE,
					ModBlockTags.SUPPORT_WIRE_CONNECTABLE,
					ModBlockTags.CANTILEVER_MAST_HINGE
			},
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
			.loot((table, block) -> table.add(block, LootTable.lootTable()
					.withPool(LootPool.lootPool()
							.setRolls(ConstantValue.exactly(1.0f))
							.when(ExplosionCondition.survivesExplosion())
							.add(LootItem.lootTableItem(block)
									.when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
											.setProperties(StatePropertiesPredicate.Builder.properties()
													.hasProperty(TensioningDeviceBlock.HELPER, false)
											)
									)
							)
					)
			))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.item()
			.model((c, p) -> DataGen.existingItemModel(c, p, "item/tensioning_device"))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.properties(p -> p.stacksTo(16))
			.build()
			.register();

	public static final BlockEntry<VInsulatorBlock> V_INSULATOR_BROWN = PantographsAndWires.REGISTRATE.block("v_insulator_brown", VInsulatorBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.lang("V-Shaped Insulator")
			.tag(ModBlockTags.V_SHAPED_INSULATORS, ModBlockTags.BROWN_INSULATOR)
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
			.tag(ModBlockTags.V_SHAPED_INSULATORS, ModBlockTags.GREEN_INSULATOR)
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
			.tag(ModBlockTags.I_SHAPED_INSULATORS, ModBlockTags.BROWN_INSULATOR)
			.blockstate((c, p) -> DataGen.insulatorBlock(c, p, "block/insulator", "i_insulator_brown"))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.item()
			.model((c, p) -> DataGen.itemModel(c, p, "block/insulator/base/brown_insulator"))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.tag(ModItemTags.INSULATORS)
			.build()
			.register();
	public static final BlockEntry<InsulatorBlock> INSULATOR_GREEN = PantographsAndWires.REGISTRATE.block("insulator_green", InsulatorBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.lang("Insulator")
			.tag(ModBlockTags.I_SHAPED_INSULATORS, ModBlockTags.GREEN_INSULATOR)
			.blockstate((c, p) -> DataGen.insulatorBlock(c, p, "block/insulator", "i_insulator_green"))
			.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
			.addLayer(() -> () -> RenderType.translucent())
			.item()
			.model((c, p) -> DataGen.itemModel(c, p, "block/insulator/base/green_insulator"))
			.tab(ModCreativeModeTab.MAIN_TAB.getKey())
			.tag(ModItemTags.INSULATORS)
			.build()
			.register();

	public static final BlockEntry<UInsulatorBlock> U_INSULATOR_GREEN = PantographsAndWires.REGISTRATE.block("u_insulator_green", UInsulatorBlock::new)
			.initialProperties(SharedProperties::softMetal)
			.transform(TagGen.pickaxeOnly())
			.lang("U-Shaped Insulator")
			.tag(ModBlockTags.U_SHAPED_INSULATORS, ModBlockTags.GREEN_INSULATOR)
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
			.tag(ModBlockTags.U_SHAPED_INSULATORS, ModBlockTags.BROWN_INSULATOR)
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
					.lang("Cantilever")
					.transform(TagGen.pickaxeOnly())
					.addLayer(() -> () -> RenderType.translucent())
					.register()
			);
			CANTILEVERS.put(t, cantileverBlock);

			DLUtils.doIfNotNull(cantileverBlock, x -> {
				final BlockEntry<CantileverBlock> y = x;
				ItemEntry<CantileverBlockItem<CantileverBlock>> item = PantographsAndWires.REGISTRATE.item(String.format("cantilever_%s", type.getSerializedName()), p -> new CantileverBlockItem<>(y.get(), type, p))
						.tab(ModCreativeModeTab.MAIN_TAB.getKey())
						.lang("Cantilever")
						.tag(ModItemTags.CANTILEVERS)
						.register()
						;
				CANTILEVER_ITEMS.put(type, item);
			});
		}
	}

	@FunctionalInterface
	public interface IOxidizingBlockFactory<T extends Block & IWeatheringBlock<T>> {
		T create(BlockBehaviour.Properties properties, IWeatheringBlock.WeatherData<T> ageData);
	}

	private static <T extends Block & IWeatheringBlock<T>> ImmutableMap<OxidizingKey, BlockEntry<T>> registerOxidizingBlock(
			String baseId,
			IOxidizingBlockFactory<T> factory,
			String baseName,
			MastMaterial material,
			NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> blockStateGen,
			Function<WeatherState, TagKey<Block>[]> tagsFactory,
			boolean addGalvanizedState,
			UnaryOperator<BlockBuilder<T, CreateRegistrate>> commonBuilder
	) {
		Map<OxidizingKey, BlockEntry<T>> variants = new HashMap<>();
		Map<WeatherState, AtomicReference<BlockEntry<T>>> waxRefs = new HashMap<>();

		// Pass 1: Unwaxed
		BlockEntry<T> nextEntry = null;
		for (int i = WeatherState.values().length - 1; i >= 0; i--) {
			WeatherState s = WeatherState.values()[i];
			if (s == WeatherState.GALVANIZED && !addGalvanizedState) continue;

			final BlockEntry<T> next = nextEntry;
			final AtomicReference<BlockEntry<T>> waxRef = new AtomicReference<>(null);

			if (s.canOxidize()) {
				waxRefs.put(s, waxRef);
			}

			final WeatherState previousState = getPreviousWeatherState(s, addGalvanizedState);

			IWeatheringBlock.WeatherData<T> ageData = new IWeatheringBlock.WeatherData<>(
					s,
					new IAgingBlock.BlockTransform<>(
							previousState == null ? null : () -> variants.get(new OxidizingKey(previousState, false)).get(),
							next == null ? null : next::get,
							s.canOxidize() ? () -> waxRef.get().get() : null
					),
					false
			);

			BlockEntry<T> block = buildBlock(baseId, factory, baseName, material, blockStateGen, tagsFactory, commonBuilder, s, false, ageData);

			variants.put(new OxidizingKey(s, false), block);
			if (s != WeatherState.GALVANIZED) {
				nextEntry = block;
			}
		}

		// Pass 2: Waxed (nur canOxidize-States, kein GALVANIZED)
		BlockEntry<T> nextWaxed = null;
		for (int i = WeatherState.oxidationStates().length - 1; i >= 0; i--) {
			WeatherState s = WeatherState.oxidationStates()[i];

			final BlockEntry<T> next = nextWaxed;
			final BlockEntry<T> unwaxedCounterpart = variants.get(new OxidizingKey(s, false));
			final WeatherState previousState = getPreviousOxidationState(s);

			IWeatheringBlock.WeatherData<T> ageData = new IWeatheringBlock.WeatherData<>(
					s,
					new IAgingBlock.BlockTransform<>(
							previousState == null ? null : () -> variants.get(new OxidizingKey(previousState, true)).get(),
							next == null ? null : next::get,
							unwaxedCounterpart::get
					),
					true
			);

			BlockEntry<T> block = buildBlock(baseId, factory, baseName, material,
					blockStateGen, tagsFactory, commonBuilder, s, true, ageData);

			waxRefs.get(s).set(block);

			variants.put(new OxidizingKey(s, true), block);
			nextWaxed = block;
		}

		return ImmutableMap.copyOf(variants);
	}

	@Nullable
	private static WeatherState getPreviousWeatherState(WeatherState s, boolean includeGalvanized) {
		if (s == WeatherState.GALVANIZED) {
			return null;
		}
		WeatherState[] all = WeatherState.values();
		int idx = s.ordinal();
		for (int i = idx - 1; i >= 0; i--) {
			WeatherState candidate = all[i];
			if (candidate == WeatherState.GALVANIZED && !includeGalvanized) continue;
			return candidate;
		}
		return null;
	}

	@Nullable
	private static WeatherState getPreviousOxidationState(WeatherState s) {
		WeatherState[] states = WeatherState.oxidationStates();
		int idx = -1;
		for (int i = 0; i < states.length; i++) {
			if (states[i] == s) { idx = i; break; }
		}
		return idx <= 0 ? null : states[idx - 1];
	}

	@SuppressWarnings("all")
	private static <T extends Block & IWeatheringBlock<T>> BlockEntry<T> buildBlock(
			String baseId,
			IOxidizingBlockFactory<T> factory,
			String baseName,
			MastMaterial material,
			NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> blockStateGen,
			Function<WeatherState, TagKey<Block>[]> tagsFactory,
			UnaryOperator<BlockBuilder<T, CreateRegistrate>> commonBuilder,
			WeatherState s,
			boolean isWaxed,
			IWeatheringBlock.WeatherData<T> ageData
	) {
		String id = (isWaxed ? "waxed_" : "") + baseId + (!s.getName().isBlank() ? "_" + s.getName() : "");
		String materialName = material.getWeatherStateName(s);

		TagKey[] tags = {
				switch (s) {
					case EXPOSED    -> ModBlockTags.EXPOSED_MASTS;
					case WEATHERED  -> ModBlockTags.WEATHERED_MASTS;
					case OXIDIZED   -> ModBlockTags.OXIDIZED_MASTS;
					case GALVANIZED -> ModBlockTags.GALVANIZED_MASTS;
					default         -> ModBlockTags.RAW_MASTS;
				},
				isWaxed ? ModBlockTags.WAXED_MASTS : ModBlockTags.UNWAXED_MASTS
		};

		BlockEntry<T> block = commonBuilder.apply(
				PantographsAndWires.REGISTRATE.block(id, p -> factory.create(p, ageData))
						.lang((isWaxed ? "Waxed " : "") + (!materialName.isBlank() ? materialName + " " : "") + baseName)
						.blockstate(blockStateGen)
						.properties(p -> (!isWaxed && s.canOxidize() && s != WeatherState.OXIDIZED) ? p.randomTicks() : p)
						.tag(tags)
						.tag(tagsFactory.apply(s))
		).register();

		if (Platform.getEnvironment() == Env.CLIENT) {
			DLBlockModelRegistry.registerForBlock(block::get, BasicRotatableBlockModel::new, BasicRotatableBlockModel::new);
		}

		return block;
	}

	private static <T extends BlockEntry<? extends Block>> T registerBlockEntityBlock(Collection<NonNullSupplier<? extends Block>> mem, T e) {
		mem.add(e);
		return e;
	}
}