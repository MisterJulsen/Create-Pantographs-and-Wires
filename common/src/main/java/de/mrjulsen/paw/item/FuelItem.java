package de.mrjulsen.paw.item;

import dev.architectury.registry.fuel.FuelRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class FuelItem extends Item {

    private int burnTime = 0;

    public FuelItem(Properties properties) {
        super(properties);
    }
    
    public void setBurnTime(int burnTime) {
		FuelRegistry.register(burnTime, this);
		this.burnTime = burnTime;
	}

	public int getBurnTime(ItemStack itemStack, RecipeType<?> recipeType) {
		return this.burnTime;
	}
    
}
