package de.mrjulsen.paw.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.AllInteractionBehaviours;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
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
import de.mrjulsen.paw.block.model.OxidizedBlockModel;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.blockentity.PantographInteractionBehaviour;
import de.mrjulsen.paw.blockentity.PantographMovementBehaviour;
import de.mrjulsen.paw.client.model.RotatedBlockModel;
import de.mrjulsen.paw.item.CantileverBlockItem;
import de.mrjulsen.paw.item.FuelBlockItem;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import de.mrjulsen.mcdragonlib.client.model.CustomBlockModelRegistry;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

public class ModBlocks {	

	private static TagKey<Block> createTag(String name) {
		return TagKey.create(Registries.BLOCK, new ResourceLocation(PantographsAndWires.MOD_ID, name));
	}

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
	
	public static final TagKey<Block> TAG_CATENARY_HEADSPAN_CONNECTABLE = createTag("catenary_headspan_connectable");

	public static final BlockEntry<PantographBlock> PANTOGRAPH = PantographsAndWires.REGISTRATE.block("pantograph", PantographBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllMovementBehaviours.movementBehaviour(new PantographMovementBehaviour()))
		.onRegister(AllInteractionBehaviours.interactionBehaviour(new PantographInteractionBehaviour()))
		.register();
		
	public static final ImmutableMap<WeatherState, BlockEntry<LatticeMastBlock>> LATTICE_MAST = registerOxidizingBlock("lattice_mast", LatticeMastBlock::new, Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal")), true, builder -> builder
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
	);

	public static final ImmutableMap<WeatherState, BlockEntry<FlatLatticeMastBlock>> FLAT_LATTICE_MAST = registerOxidizingBlock("flat_lattice_mast", FlatLatticeMastBlock::new, Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal")), true, builder -> builder
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
	);

	public static final ImmutableMap<WeatherState, BlockEntry<FlatLatticeMastBlock>> FLAT_LATTICE_MAST_DIAGONAL = registerOxidizingBlock("flat_lattice_mast_diagonal", FlatLatticeMastBlock::new, Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal")), true, builder -> builder
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
	);		

	public static final ImmutableMap<WeatherState, BlockEntry<HBeamMastBlock>> H_BEAM_MAST = registerOxidizingBlock("h_beam_mast", HBeamMastBlock::new, Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal")), true, builder -> builder
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
	);

	public static final ImmutableMap<WeatherState, BlockEntry<ConcretePillarBlock>> CONCRETE_POST = registerOxidizingBlock("concrete_post", (properties, weatherState, next) -> new ConcretePillarBlock(properties, weatherState, next, false), Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/concrete_post")), false, builder -> builder
		.initialProperties(() -> Blocks.SMOOTH_STONE)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
	);

	public static final ImmutableMap<WeatherState, BlockEntry<ConcretePillarBlock>> CONCRETE_PILLAR = registerOxidizingBlock("concrete_pillar", (properties, weatherState, next) -> new ConcretePillarBlock(properties, weatherState, next, true), Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/concrete_post")), false, builder -> builder
		.initialProperties(() -> Blocks.SMOOTH_STONE)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
	);



	public static final BlockEntry<RegistrationArmBlock> REGISTRATION_ARM = PantographsAndWires.REGISTRATE.block("registration_arm", RegistrationArmBlock::new)
		.register();

	public static final BlockEntry<TensioningDeviceBlock> TENSIONING_DEVICE = PantographsAndWires.REGISTRATE.block("tensioning_device", TensioningDeviceBlock::new)
		.initialProperties(() -> Blocks.SMOOTH_STONE)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.properties(p -> p.stacksTo(16))
		.build()
		.register();


	public static final ImmutableMap<WeatherState, BlockEntry<CantileverBracketBlock>> CANTILEVER_BRACKET = registerOxidizingBlock("cantilever_bracket", CantileverBracketBlock::new, Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal")), true, builder -> builder
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
	);
	public static final ImmutableMap<WeatherState, BlockEntry<CantileverBracketVerticalBlock>> CANTILEVER_BRACKET_VERTICAL = registerOxidizingBlock("cantilever_bracket_vertical", CantileverBracketVerticalBlock::new, Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal")), true, builder -> builder
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
	);
	public static final ImmutableMap<WeatherState, BlockEntry<CantileverBracketPostConnectionBlock>> CANTILEVER_BRACKET_AT_POST = registerOxidizingBlock("cantilever_bracket_at_post", CantileverBracketPostConnectionBlock::new, Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal")), true, builder -> builder
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
	);


		
	public static final ImmutableMap<WeatherState, BlockEntry<PowerLineBracketBlock>> POWER_LINE_BRACKET = registerOxidizingBlock("power_line_bracket", PowerLineBracketBlock::new, Set.of(new ResourceLocation(PantographsAndWires.MOD_ID, "block/metal")), true, builder -> builder
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
	);

		
	public static final BlockEntry<VInsulatorBlock> V_INSULATOR_BROWN = PantographsAndWires.REGISTRATE.block("v_insulator_brown", VInsulatorBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();		
	public static final BlockEntry<VInsulatorBlock> V_INSULATOR_GREEN = PantographsAndWires.REGISTRATE.block("v_insulator_green", VInsulatorBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.addLayer(() -> () -> RenderType.translucent())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();
		
	public static final BlockEntry<InsulatorBlock> INSULATOR_BROWN = PantographsAndWires.REGISTRATE.block("insulator_brown", InsulatorBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();
	public static final BlockEntry<InsulatorBlock> INSULATOR_GREEN = PantographsAndWires.REGISTRATE.block("insulator_green", InsulatorBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.addLayer(() -> () -> RenderType.translucent())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

	public static final BlockEntry<UInsulatorBlock> U_INSULATOR_GREEN = PantographsAndWires.REGISTRATE.block("u_insulator_green", UInsulatorBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.addLayer(() -> () -> RenderType.translucent())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();		
	public static final BlockEntry<UInsulatorBlock> U_INSULATOR_BROWN = PantographsAndWires.REGISTRATE.block("u_insulator_brown", UInsulatorBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

	public static final BlockEntry<Block> GRAPHITE_BLOCK = PantographsAndWires.REGISTRATE.block("graphite_block", Block::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

	public static final BlockEntry<Block> COAL_COKE_BLOCK = PantographsAndWires.REGISTRATE.block("coal_coke_block", Block::new)
		.initialProperties(() -> Blocks.DEEPSLATE)
		.transform(TagGen.pickaxeOnly())
		.item(FuelBlockItem::new)
		.onRegister((item) -> item.setBurnTime(32000))
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
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
		T create(Properties properties, WeatherState state, Supplier<T> oxidationStates);
	}
	private static <T extends Block & IWeatheringBlock<T>> ImmutableMap<WeatherState, BlockEntry<T>> registerOxidizingBlock(String name, IOxidizingBlockFactory<T> factory, Set<ResourceLocation> oxidizingTextures, boolean addTreatedState, UnaryOperator<BlockBuilder<T, CreateRegistrate>> builder) {
		AtomicReference<BlockEntry<T>> previous = new AtomicReference<>(null);
		Map<WeatherState, BlockEntry<T>> variants = new HashMap<>(WeatherState.values().length);
		for (int i = WeatherState.oxidationStates().length - 1; i >= 0; i--) {
			final WeatherState s = WeatherState.oxidationStates()[i];
			final BlockEntry<T> prev = previous.get();
			BlockEntry<T> block = builder.apply(PantographsAndWires.REGISTRATE.block(name + (!s.getname().isBlank() ? "_" + s.getname() : ""), p -> factory.create(p, s, () -> prev == null ? null : prev.get())))
				.register()
			;
			if (Platform.getEnvironment() == Env.CLIENT) {
				CustomBlockModelRegistry.registerForBlock(() -> block.get(), () -> new OxidizedBlockModel(oxidizingTextures), () -> new OxidizedBlockModel(oxidizingTextures));
			}
			variants.put(s, block);
			previous.set(block);
		}

		if (addTreatedState) {
			final WeatherState s = WeatherState.TREATED;
			BlockEntry<T> block = builder.apply(PantographsAndWires.REGISTRATE.block(name + (!s.getname().isBlank() ? "_" + s.getname() : ""), p -> factory.create(p, s, null)))
				.register()
			;
			if (Platform.getEnvironment() == Env.CLIENT) {
				CustomBlockModelRegistry.registerForBlock(() -> block.get(), () -> new OxidizedBlockModel(oxidizingTextures), () -> new OxidizedBlockModel(oxidizingTextures));
			}
			variants.put(s, block);
		}
		return ImmutableMap.copyOf(variants);
	}
	
	

	private static <T extends BlockEntry<? extends Block>> T registerBlockEntityBlock(Collection<NonNullSupplier<? extends Block>> mem, T e) {
		mem.add(e);
		return e;
	}
}
