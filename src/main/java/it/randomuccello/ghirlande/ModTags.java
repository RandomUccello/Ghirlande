package it.randomuccello.ghirlande;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final TagKey<Item> RED_FLOWERS = tag("red_flowers");
    public static final TagKey<Item> YELLOW_FLOWERS = tag("yellow_flowers");
    public static final TagKey<Item> BLUE_FLOWERS = tag("blue_flowers");
    public static final TagKey<Item> WHITE_FLOWERS = tag("white_flowers");
    public static final TagKey<Item> ORANGE_FLOWERS = tag("orange_flowers");
    public static final TagKey<Item> PINK_FLOWERS = tag("pink_flowers");
    public static final TagKey<Item> MAGENTA_FLOWERS = tag("magenta_flowers");
    public static final TagKey<Item> LIGHT_BLUE_FLOWERS = tag("light_blue_flowers");
    public static final TagKey<Item> LIGHT_GRAY_FLOWERS = tag("light_gray_flowers");
    public static final TagKey<Item> CYAN_FLOWERS = tag("cyan_flowers");
    public static final TagKey<Item> BLACK_FLOWERS = tag("black_flowers");
    public static final TagKey<Item> GRAY_FLOWERS = tag("gray_flowers");

    private ModTags() {
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, GhirlandeMod.id(path));
    }
}
