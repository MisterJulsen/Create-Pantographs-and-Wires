package de.mrjulsen.paw.item.neoforge;

import java.util.function.Consumer;

import de.mrjulsen.mcdragonlib.util.DLUtils;
import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.item.PantographItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PantographItemImpl extends PantographItem {

	protected PantographItemImpl(Block block, Properties properties, boolean expanded) {
		super(block, properties, expanded);
	}

	public static PantographItem create(Block block, Properties properties, boolean expanded) {
        return new PantographItemImpl(block, properties, expanded);
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(new IClientItemExtensions() {
			private GeoItemRenderer<PantographItem> renderer = null;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (this.renderer == null)
					this.renderer = new GeoItemRenderer<>(new DefaultedBlockGeoModel<>(DLUtils.resourceLocation(PantographsAndWires.MOD_ID, "pantograph")));

				return this.renderer;
			}
		});
	}    
}
