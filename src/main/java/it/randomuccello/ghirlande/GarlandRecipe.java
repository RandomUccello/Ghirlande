package it.randomuccello.ghirlande;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class GarlandRecipe extends CustomRecipe {
    public static final MapCodec<GarlandRecipe> CODEC = MapCodec.unit(new GarlandRecipe());
    public static final StreamCodec<RegistryFriendlyByteBuf, GarlandRecipe> STREAM_CODEC = StreamCodec.unit(new GarlandRecipe());

    public GarlandRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }

        if (!input.getItem(4).isEmpty()) {
            return false;
        }

        for (int slot = 0; slot < 9; slot++) {
            if (slot == 4) continue;
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty() || GarlandData.classifyFlower(stack) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        List<ItemStack> flowers = new ArrayList<>(8);
        GarlandColor commonColor = null;
        boolean mixed = false;

        for (int slot = 0; slot < 9; slot++) {
            if (slot == 4) continue;
            ItemStack stack = input.getItem(slot);
            GarlandColor color = GarlandData.classifyFlower(stack);
            if (color == null) {
                return ItemStack.EMPTY;
            }
            flowers.add(stack);
            if (commonColor == null) {
                commonColor = color;
            } else if (commonColor != color) {
                mixed = true;
            }
        }

        GarlandColor resultColor = mixed || commonColor == null ? GarlandColor.MIXED : commonColor;
        ItemStack output = new ItemStack(ModItems.GARLAND);
        GarlandData.writeFromCrafting(output, flowers, resultColor);
        return output;
    }

    public ItemStack getResultItem() {
        return new ItemStack(ModItems.GARLAND);
    }

    @Override
    public RecipeSerializer<GarlandRecipe> getSerializer() {
        return ModRecipes.GARLAND;
    }
}
