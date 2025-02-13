package de.mrjulsen.paw.item;

import dev.architectury.registry.fuel.FuelRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

public class FuelBlockItem extends BlockItem {

	private int burnTime = 0;

	public FuelBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

    public void setBurnTime(int burnTime) {
		FuelRegistry.register(burnTime, this);
		this.burnTime = burnTime;
	}

	public int getBurnTime(ItemStack itemStack, RecipeType<?> recipeType) {
		return this.burnTime;
	}
    
}
