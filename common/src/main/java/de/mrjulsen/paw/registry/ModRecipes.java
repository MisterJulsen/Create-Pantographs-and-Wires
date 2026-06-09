package de.mrjulsen.paw.registry;

import de.mrjulsen.paw.PantographsAndWires;
import de.mrjulsen.paw.data.recipes.CombineWireRecipe;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
<<<<<<< HEAD
import net.minecraft.core.registries.Registries;
=======
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
>>>>>>> 8df5b91ab8296faa4d4b83d29b46cba3751d2e5d
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
