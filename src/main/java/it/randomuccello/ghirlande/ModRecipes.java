package it.randomuccello.ghirlande;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipes {
    public static final RecipeSerializer<GarlandRecipe> GARLAND =
            new RecipeSerializer<>(GarlandRecipe.CODEC, GarlandRecipe.STREAM_CODEC);

    private ModRecipes() {
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, GhirlandeMod.id("garland"), GARLAND);
    }
}
