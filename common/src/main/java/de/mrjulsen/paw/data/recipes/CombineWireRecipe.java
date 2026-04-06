package de.mrjulsen.paw.data.recipes;

import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.paw.registry.ModRecipes;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CombineWireRecipe extends CustomRecipe {

    public CombineWireRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int wireCount = 0;
        Item wireType = null;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
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
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        int totalWire = 0;
        ItemStack firstWire = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
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

        int outputAmount = Math.min(totalWire, IPawWireItemBase.WIRE_LENGTH);
        firstWire.setCount(1);
        firstWire.getOrCreateTag().putInt(IPawWireItemBase.NBT_WIRE_LENGTH, outputAmount);

        return firstWire;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        int totalWire = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MultiWireItem) {
                totalWire += IPawWireItemBase.getRemainingWire(stack);
            }
        }

        int leftover = totalWire - IPawWireItemBase.WIRE_LENGTH;
        boolean skippedFirst = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof MultiWireItem) {
                if (!skippedFirst) {
                    skippedFirst = true;
                    remaining.set(i, ItemStack.EMPTY);
                } else {
                    if (leftover > 0) {
                        int amount = Math.min(leftover, IPawWireItemBase.WIRE_LENGTH);
                        ItemStack remainingWire = new ItemStack(stack.getItem());
                        remainingWire.getOrCreateTag().putInt(IPawWireItemBase.NBT_WIRE_LENGTH, amount);
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