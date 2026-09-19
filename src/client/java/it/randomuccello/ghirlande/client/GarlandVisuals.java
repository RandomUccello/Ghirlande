package it.randomuccello.ghirlande.client;

import it.randomuccello.ghirlande.GarlandData;
import it.randomuccello.ghirlande.GhirlandeMod;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Visual-only helpers for the modular garland prototype.
 *
 * The graphics prototype deliberately supports only dandelion and poppy with
 * custom sprites. Unsupported flowers return an empty visual instead of
 * falling back to vanilla item models, preventing accidental 3D flowers from
 * leaking into the new flat rendering pipeline.
 */
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

    static ItemStack headFor(String rawId) {
        return headFor(rawId, false);
    }

    static ItemStack fadedHeadFor(String rawId) {
        return headFor(rawId, true);
    }

    static ItemStack vineIcon() {
        return modelStack("visual_vine_icon");
    }

    static ItemStack vineSegment() {
        return modelStack("visual_vine_segment");
    }

    private static ItemStack headFor(String rawId, boolean faded) {
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

        return ItemStack.EMPTY;
    }

    private static ItemStack modelStack(String modelId) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL, GhirlandeMod.id(modelId));
        return stack;
    }
}
