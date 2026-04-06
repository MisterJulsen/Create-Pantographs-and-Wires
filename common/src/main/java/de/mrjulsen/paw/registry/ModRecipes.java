package de.mrjulsen.paw.registry;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.data.recipes.CombineWireRecipe;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public final class ModRecipes {
    private ModRecipes() {}

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(PantographsAndWires.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<CombineWireRecipe>> COMBINE_WIRE = SERIALIZERS.register("combine_wire", () -> new SimpleCraftingRecipeSerializer<>(CombineWireRecipe::new));

    public static void init() {
        SERIALIZERS.register();
    }
}
