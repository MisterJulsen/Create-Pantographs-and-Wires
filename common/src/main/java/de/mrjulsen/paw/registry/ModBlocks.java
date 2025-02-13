package de.mrjulsen.paw.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.simibubi.create.AllInteractionBehaviours;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.block.CantileverBlock;
import de.mrjulsen.paw.block.CantileverBracketBlock;
import de.mrjulsen.paw.block.CantileverBracketPostConnectionBlock;
import de.mrjulsen.paw.block.CantileverBracketVerticalBlock;
import de.mrjulsen.paw.block.ConcretePillarBlock;
import de.mrjulsen.paw.block.DoubleCantileverBlock;
import de.mrjulsen.paw.block.FlatLatticeMastBlock;
import de.mrjulsen.paw.block.HBeamMastBlock;
import de.mrjulsen.paw.block.InsulatorBlock;
import de.mrjulsen.paw.block.LatticeMastBlock;
import de.mrjulsen.paw.block.PantographBlock;
import de.mrjulsen.paw.block.PowerLineBracketBlock;
import de.mrjulsen.paw.block.TensioningDeviceBlock;
import de.mrjulsen.paw.block.UInsulatorBlock;
import de.mrjulsen.paw.block.VInsulatorBlock;
import de.mrjulsen.paw.block.abstractions.AbstractCantileverBlock;
import de.mrjulsen.paw.block.property.EInsulatorType;
import de.mrjulsen.paw.blockentity.PantographInteractionBehaviour;
import de.mrjulsen.paw.blockentity.PantographMovementBehaviour;
import de.mrjulsen.paw.client.model.RotatedBlockModel;
import de.mrjulsen.paw.item.CantileverBlockItem;
import de.mrjulsen.paw.item.FuelBlockItem;
import de.mrjulsen.mcdragonlib.util.DLUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ModBlocks {	

	private static TagKey<Block> create(String name) {
		return TagKey.create(Registries.BLOCK, new ResourceLocation(PantographsAndWires.MOD_ID, name));
	}

	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE = create("cantilever_connectable");
	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_16PX = create("cantilever_connectable_16px");
	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_12PX = create("cantilever_connectable_12px");
	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_8PX = create("cantilever_connectable_8px");
	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_5PX = create("cantilever_connectable_5px");
	public static final TagKey<Block> TAG_CANTILEVER_CONNECTABLE_4PX = create("cantilever_connectable_4px");
	public static final TagKey<Block> TAG_TENSIONING_DEVICE_CONNECTABLE = create("tensioning_device_connectable");

	public static final BlockEntry<PantographBlock> PANTOGRAPH = PantographsAndWires.REGISTRATE.block("pantograph", PantographBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(AllMovementBehaviours.movementBehaviour(new PantographMovementBehaviour()))
		.onRegister(AllInteractionBehaviours.interactionBehaviour(new PantographInteractionBehaviour()))
		.register();

	public static final BlockEntry<LatticeMastBlock> LATTICE_MAST = PantographsAndWires.REGISTRATE.block("lattice_mast", LatticeMastBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

	public static final BlockEntry<FlatLatticeMastBlock> FLAT_LATTICE_MAST = PantographsAndWires.REGISTRATE.block("flat_lattice_mast", FlatLatticeMastBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

	public static final BlockEntry<FlatLatticeMastBlock> FLAT_LATTICE_MAST_DIAGONAL = PantographsAndWires.REGISTRATE.block("flat_lattice_mast_diagonal", FlatLatticeMastBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();		

	public static final BlockEntry<HBeamMastBlock> H_BEAM_MAST = PantographsAndWires.REGISTRATE.block("h_beam_mast", HBeamMastBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

	public static final BlockEntry<ConcretePillarBlock> CONCRETE_POST = PantographsAndWires.REGISTRATE.block("concrete_post", p -> new ConcretePillarBlock(p, false))
		.initialProperties(() -> Blocks.SMOOTH_STONE)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

	public static final BlockEntry<ConcretePillarBlock> CONCRETE_PILLAR = PantographsAndWires.REGISTRATE.block("concrete_pillar", p -> new ConcretePillarBlock(p, true))
		.initialProperties(() -> Blocks.SMOOTH_STONE)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
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


	public static final BlockEntry<CantileverBracketBlock> CANTILEVER_BRACKET = PantographsAndWires.REGISTRATE.block("cantilever_bracket", CantileverBracketBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();
	public static final BlockEntry<CantileverBracketVerticalBlock> CANTILEVER_BRACKET_VERTICAL = PantographsAndWires.REGISTRATE.block("cantilever_bracket_vertical", CantileverBracketVerticalBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.register();		
	public static final BlockEntry<CantileverBracketPostConnectionBlock> CANTILEVER_BRACKET_AT_POST = PantographsAndWires.REGISTRATE.block("cantilever_bracket_at_post", CantileverBracketPostConnectionBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.register();

// overhead line gantry
// overhead cross span


		
	public static final BlockEntry<PowerLineBracketBlock> POWER_LINE_BRACKET = PantographsAndWires.REGISTRATE.block("power_line_bracket", PowerLineBracketBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

		
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
		.item()
		.tab(ModCreativeModeTab.MAIN_TAB.getKey())
		.build()
		.register();

	public static final BlockEntry<UInsulatorBlock> U_INSULATOR_GREEN = PantographsAndWires.REGISTRATE.block("u_insulator_green", UInsulatorBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.transform(TagGen.pickaxeOnly())
		.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
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
		registerDoubleCantilevers();
	}

	public static record CantileverKey(int size, EInsulatorType insulatorType) {
		@Override
		public final boolean equals(Object arg0) {
			return arg0 instanceof CantileverKey o && size == o.size && insulatorType == o.insulatorType;
		}

		@Override
		public final int hashCode() {
			return Objects.hash(size, insulatorType);
		}
	}

	public static final Map<CantileverKey, BlockEntry<? extends AbstractCantileverBlock>> CANTILEVERS = new HashMap<>();
	public static final Map<CantileverKey, BlockEntry<? extends DoubleCantileverBlock>> DOUBLE_CANTILEVERS = new HashMap<>();
	public static final Collection<NonNullSupplier<? extends Block>> CANTILEVER_BLOCK_ENTITY_BLOCKS = new ArrayList<>();
	public static final Map<EInsulatorType, ItemEntry<CantileverBlockItem<CantileverBlock>>> CANTILEVER_ITEMS = new HashMap<>();

	public static BlockEntry<? extends AbstractCantileverBlock> getCantilever(CantileverKey key) {
		return CANTILEVERS.get(key);
	}

	public static Collection<BlockEntry<? extends AbstractCantileverBlock>> getCantilevers() {
		return CANTILEVERS.values();
	}

	public static BlockEntry<? extends DoubleCantileverBlock> getDoubleCantilever(CantileverKey key) {
		return DOUBLE_CANTILEVERS.get(key);
	}

	public static Collection<BlockEntry<? extends DoubleCantileverBlock>> getDoubleCantilevers() {
		return DOUBLE_CANTILEVERS.values();
	}

	private static void registerCantilevers() {
		for (EInsulatorType type : EInsulatorType.values()) {
			BlockEntry<CantileverBlock> firstBlock = null;
			for (byte i = 3; i <= AbstractCantileverBlock.MAX_SIZE; i++) {
				final byte k = i;
				final EInsulatorType t = type;
				BlockEntry<CantileverBlock> b = registerBlockEntityBlock(CANTILEVER_BLOCK_ENTITY_BLOCKS, PantographsAndWires.REGISTRATE.block(String.format("cantilever_%s_%s", k, type.getSerializedName()), p -> new CantileverBlock(p, k, t))
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
					.addLayer(() -> () -> RenderType.translucent())
					.register()
				);
				CANTILEVERS.put(new CantileverKey(i, type), b);

				if (firstBlock == null) {
					firstBlock = b;
				}
			}

			DLUtils.doIfNotNull(firstBlock, x -> {	
				final BlockEntry<CantileverBlock> y = x;
				ItemEntry<CantileverBlockItem<CantileverBlock>> item = PantographsAndWires.REGISTRATE.item(String.format("cantilever_%s", type.getSerializedName()), p -> new CantileverBlockItem<>(y.get(), type, p))
					.tab(ModCreativeModeTab.MAIN_TAB.getKey())
					.register()
				;
				CANTILEVER_ITEMS.put(type, item);
			});
		}
	}
	
	private static void registerDoubleCantilevers() {
		for (EInsulatorType type : EInsulatorType.values()) {
			for (byte i = 3; i <= AbstractCantileverBlock.MAX_SIZE; i++) {
				final byte k = i;
				final EInsulatorType t = type;
				DOUBLE_CANTILEVERS.put(new CantileverKey(i, type), registerBlockEntityBlock(CANTILEVER_BLOCK_ENTITY_BLOCKS, PantographsAndWires.REGISTRATE.block(String.format("cantilever_double_%s_%s", k, type.getSerializedName()), p -> new DoubleCantileverBlock(p, k, t))
					.initialProperties(SharedProperties::softMetal)
					.transform(TagGen.pickaxeOnly())
					.onRegister(CreateRegistrate.blockModel(() -> RotatedBlockModel::new))
					.addLayer(() -> () -> RenderType.translucent())
					.register()
				));
			}
		}
	}
	
	

	private static <T extends BlockEntry<? extends Block>> T registerBlockEntityBlock(Collection<NonNullSupplier<? extends Block>> mem, T e) {
		mem.add(e);
		return e;
	}
}
