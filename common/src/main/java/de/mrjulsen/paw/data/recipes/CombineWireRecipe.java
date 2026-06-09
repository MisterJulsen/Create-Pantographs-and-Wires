package de.mrjulsen.paw.data.recipes;

<<<<<<< HEAD
=======
import de.mrjulsen.paw.components.WireAmountComponent;
import de.mrjulsen.paw.registry.ModDataComponents;
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
import de.mrjulsen.paw.registry.ModItems;
import de.mrjulsen.paw.registry.ModRecipes;
import de.mrjulsen.wires.item.IPawWireItemBase;
import de.mrjulsen.wires.item.MultiWireItem;
<<<<<<< HEAD
=======
import net.minecraft.core.HolderLookup;
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
<<<<<<< HEAD
=======
import net.minecraft.world.item.crafting.CraftingInput;
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CombineWireRecipe extends CustomRecipe {

<<<<<<< HEAD
    public CombineWireRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int wireCount = 0;
        Item wireType = null;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
=======
    public CombineWireRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int wireCount = 0;
        Item wireType = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
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
<<<<<<< HEAD
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        int totalWire = 0;
        ItemStack firstWire = ItemStack.EMPTY;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
=======
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        int totalWire = 0;
        ItemStack firstWire = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
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

<<<<<<< HEAD
        int outputAmount = Math.min(totalWire, IPawWireItemBase.WIRE_LENGTH);
        firstWire.setCount(1);
        firstWire.getOrCreateTag().putInt(IPawWireItemBase.NBT_WIRE_LENGTH, outputAmount);
=======
        int outputAmount = Math.min(totalWire, WireAmountComponent.MAX_WIRE);
        firstWire.setCount(1);
        ModDataComponents.setComponent(firstWire, ModDataComponents.WIRE_AMOUNT, new WireAmountComponent(outputAmount));
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d

        return firstWire;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
<<<<<<< HEAD
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        int totalWire = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
=======
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        int totalWire = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
            if (!stack.isEmpty() && stack.getItem() instanceof MultiWireItem) {
                totalWire += IPawWireItemBase.getRemainingWire(stack);
            }
        }

<<<<<<< HEAD
        int leftover = totalWire - IPawWireItemBase.WIRE_LENGTH;
        boolean skippedFirst = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
=======
        int leftover = totalWire - WireAmountComponent.MAX_WIRE;
        boolean skippedFirst = false;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
            if (!stack.isEmpty() && stack.getItem() instanceof MultiWireItem) {
                if (!skippedFirst) {
                    skippedFirst = true;
                    remaining.set(i, ItemStack.EMPTY);
                } else {
                    if (leftover > 0) {
<<<<<<< HEAD
                        int amount = Math.min(leftover, IPawWireItemBase.WIRE_LENGTH);
                        ItemStack remainingWire = new ItemStack(stack.getItem());
                        remainingWire.getOrCreateTag().putInt(IPawWireItemBase.NBT_WIRE_LENGTH, amount);
=======
                        int amount = Math.min(leftover, WireAmountComponent.MAX_WIRE);
                        ItemStack remainingWire = new ItemStack(stack.getItem());
                        ModDataComponents.setComponent(remainingWire, ModDataComponents.WIRE_AMOUNT, new WireAmountComponent(amount));
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
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