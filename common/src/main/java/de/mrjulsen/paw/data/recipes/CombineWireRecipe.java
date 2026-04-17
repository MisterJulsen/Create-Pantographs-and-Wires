package de.mrjulsen.paw.data.recipes;

import de.mrjulsen.paw.components.WireAmountComponent;
import de.mrjulsen.paw.registry.ModDataComponents;
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.paw.registry.ModRecipes;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CombineWireRecipe extends CustomRecipe {

    public CombineWireRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int wireCount = 0;
        Item wireType = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() instanceof MultiWireItem) {
                    if (wireType == null) {
                        wireType = stack.getItem();
                    } else if (wireType != stack.getItem()) {
                        return false;
                    }
                    wireCount++;
                } else {
                    return false;
                }
            }
        }

        return wireCount >= 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int totalWire = 0;
        ItemStack firstWire = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MultiWireItem) {
                totalWire += IPawWireItemBase.getRemainingWire(stack);
                if (firstWire.isEmpty()) {
                    firstWire = stack.copy();
                }
            }
        }

        if (firstWire.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int outputAmount = Math.min(totalWire, WireAmountComponent.MAX_WIRE);
        firstWire.setCount(1);
        ModDataComponents.setComponent(firstWire, ModDataComponents.WIRE_AMOUNT, new WireAmountComponent(outputAmount));

        return firstWire;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        int totalWire = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MultiWireItem) {
                totalWire += IPawWireItemBase.getRemainingWire(stack);
            }
        }

        int leftover = totalWire - WireAmountComponent.MAX_WIRE;
        boolean skippedFirst = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MultiWireItem) {
                if (!skippedFirst) {
                    skippedFirst = true;
                    remaining.set(i, ItemStack.EMPTY);
                } else {
                    if (leftover > 0) {
                        int amount = Math.min(leftover, WireAmountComponent.MAX_WIRE);
                        ItemStack remainingWire = new ItemStack(stack.getItem());
                        ModDataComponents.setComponent(remainingWire, ModDataComponents.WIRE_AMOUNT, new WireAmountComponent(amount));
                        remaining.set(i, remainingWire);
                        leftover -= amount;
                    } else {
                        remaining.set(i, ModItems.EMPTY_WIRE_COIL.asStack());
                    }
                }
            }
        }

        return remaining;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COMBINE_WIRE.get();
    }
}