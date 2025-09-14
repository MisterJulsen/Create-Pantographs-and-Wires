package de.mrjulsen.paw.registry;

import java.util.Collection;
import java.util.List;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.blockentity.MultiblockWireConnectorBlockEntity;
import de.mrjulsen.paw.blockentity.PantographBlockEntity;
import de.mrjulsen.paw.blockentity.client.CantileverBlockRenderer;
import de.mrjulsen.paw.blockentity.client.PantographBlockRenderer;
import de.mrjulsen.wires.block.WireConnectorBlockEntity;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unchecked")
public class ModBlockEntities {
    
	public static final BlockEntityEntry<CantileverBlockEntity> CANTILEVER_BLOCK_ENTITY;
	public static final BlockEntityEntry<MultiblockWireConnectorBlockEntity> MULTIBLOCK_WIRE_CONNECTOR_BLOCK_ENTITY;
	public static final BlockEntityEntry<WireConnectorBlockEntity> WIRE_CONNECTOR_BLOCK_ENTITY;

	public static final BlockEntityEntry<PantographBlockEntity> PANTOGRAPH_BLOCK_ENTITY = PantographsAndWires.REGISTRATE
		.blockEntity("pantograph_block_entity", PantographBlockEntity::new)
		.validBlocks(
			ModBlocks.PANTOGRAPH
		)
		.renderer(() -> PantographBlockRenderer::new)
		.register();

	static {

		CANTILEVER_BLOCK_ENTITY = PantographsAndWires.REGISTRATE
			.blockEntity("cantilever_block_entity", CantileverBlockEntity::new)
			.validBlocks(makeArray(ModBlocks.CANTILEVER_BLOCK_ENTITY_BLOCKS))
			.renderer(() -> CantileverBlockRenderer::new)
			.register();

		MULTIBLOCK_WIRE_CONNECTOR_BLOCK_ENTITY = PantographsAndWires.REGISTRATE
			.blockEntity("multiblock_wire_connector_block_entity", MultiblockWireConnectorBlockEntity::new)
			.validBlocks(makeArray(List.of(ModBlocks.TENSIONING_DEVICE)))
			.register();

		WIRE_CONNECTOR_BLOCK_ENTITY = PantographsAndWires.REGISTRATE
			.blockEntity("wire_connector_block_entity", WireConnectorBlockEntity::new)
			.validBlocks(makeArray(ModBlocks.CANTILEVER_BLOCK_ENTITY_BLOCKS))
			.register();
		
		ModBlocks.CANTILEVER_BLOCK_ENTITY_BLOCKS.clear();	
	}

	private static NonNullSupplier<? extends Block>[] makeArray(Collection<NonNullSupplier<? extends Block>> collection) {		
		NonNullSupplier<? extends Block>[] arr = new NonNullSupplier[collection.size()];
		int i = 0;
		for (NonNullSupplier<? extends Block> block : collection) {
			arr[i] = block;
			i++;
		}
		return arr;
	}

    public static void init() { }
}
