package it.randomuccello.ghirlande;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;

public final class ModItems {
    public static final Item GARLAND = register(
            "garland",
            new Item.Properties()
                    .setId(key("garland"))
                    .stacksTo(1)
                    .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).build())
    );

    private ModItems() {
    }

    public static void initialize() {
        // Forces static registration before recipes and gameplay hooks use the item.
    }

    private static ResourceKey<Item> key(String path) {
        return ResourceKey.create(Registries.ITEM, GhirlandeMod.id(path));
    }

    private static Item register(String path, Item.Properties properties) {
        return Registry.register(BuiltInRegistries.ITEM, GhirlandeMod.id(path), new Item(properties));
    }
}
