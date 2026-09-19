package it.randomuccello.ghirlande.client;

import it.randomuccello.ghirlande.GarlandData;
import it.randomuccello.ghirlande.GhirlandeMod;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class GarlandVisuals {
    private static final Identifier DANDELION_ID =
            Identifier.fromNamespaceAndPath("minecraft", "dandelion");
    private static final Identifier POPPY_ID =
            Identifier.fromNamespaceAndPath("minecraft", "poppy");

    private GarlandVisuals() {
    }

    static List<String> flowerIds(ItemStack stack) {
        return GarlandData.fromStack(stack)
                .map(GarlandData::flowers)
                .orElse(List.of());
    }

    static ItemStack moduleFor(String rawId) {
        return moduleFor(rawId, false);
    }

    static ItemStack fadedModuleFor(String rawId) {
        return moduleFor(rawId, true);
    }

    static ItemStack vineIcon() {
        return modelStack("visual_vine_icon");
    }

    private static ItemStack moduleFor(String rawId, boolean faded) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            return ItemStack.EMPTY;
        }

        if (DANDELION_ID.equals(id)) {
            return modelStack(faded
                    ? "visual_dandelion_module_faded"
                    : "visual_dandelion_module");
        }

        if (POPPY_ID.equals(id)) {
            return modelStack(faded
                    ? "visual_poppy_module_faded"
                    : "visual_poppy_module");
        }

        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static ItemStack modelStack(String modelId) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL, GhirlandeMod.id(modelId));
        return stack;
    }
}
