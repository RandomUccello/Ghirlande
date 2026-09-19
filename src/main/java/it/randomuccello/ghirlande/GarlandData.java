package it.randomuccello.ghirlande;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record GarlandData(GarlandColor color, int charges, List<String> flowers) {
    private static final String ROOT = "ghirlande";
    private static final String COLOR = "color";
    private static final String CHARGES = "charges";
    private static final String FLOWERS = "flowers";

    public GarlandData {
        flowers = List.copyOf(flowers);
    }

    public static Optional<GarlandData> fromStack(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Optional.empty();
        }

        CompoundTag root = customData.copyTag();
        CompoundTag data = root.getCompoundOrEmpty(ROOT);
        if (data.isEmpty()) {
            return Optional.empty();
        }

        GarlandColor color = GarlandColor.byId(data.getStringOr(COLOR, GarlandColor.MIXED.id()));
        int charges = data.getIntOr(CHARGES, color.initialCharges());
        ListTag list = data.getListOrEmpty(FLOWERS);
        List<String> flowers = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            String id = list.getStringOr(i, "");
            if (!id.isBlank()) {
                flowers.add(id);
            }
        }

        return Optional.of(new GarlandData(color, charges, flowers));
    }

    public static void write(ItemStack stack, GarlandData value) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag data = new CompoundTag();
        data.putString(COLOR, value.color().id());
        data.putInt(CHARGES, value.charges());

        ListTag list = new ListTag();
        for (String flower : value.flowers()) {
            list.add(StringTag.valueOf(flower));
        }
        data.put(FLOWERS, list);
        root.put(ROOT, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static void writeFromCrafting(ItemStack output, List<ItemStack> flowers, GarlandColor color) {
        List<String> ids = flowers.stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                .toList();
        write(output, new GarlandData(color, color.initialCharges(), ids));
    }

    public static boolean hasCharge(ItemStack stack) {
        return fromStack(stack).map(data -> data.charges() > 0).orElse(false);
    }

    public static boolean consumeCharge(ItemStack stack) {
        Optional<GarlandData> current = fromStack(stack);
        if (current.isEmpty() || current.get().charges() <= 0) {
            return false;
        }
        GarlandData old = current.get();
        write(stack, new GarlandData(old.color(), old.charges() - 1, old.flowers()));
        return true;
    }

    public static GarlandColor classifyFlower(ItemStack stack) {
        if (stack.is(ModTags.RED_FLOWERS)) return GarlandColor.RED;
        if (stack.is(ModTags.YELLOW_FLOWERS)) return GarlandColor.YELLOW;
        if (stack.is(ModTags.BLUE_FLOWERS)) return GarlandColor.BLUE;
        if (stack.is(ModTags.WHITE_FLOWERS)) return GarlandColor.WHITE;
        if (stack.is(ModTags.ORANGE_FLOWERS)) return GarlandColor.ORANGE;
        if (stack.is(ModTags.PINK_FLOWERS)) return GarlandColor.PINK;
        if (stack.is(ModTags.MAGENTA_FLOWERS)) return GarlandColor.MAGENTA;
        if (stack.is(ModTags.LIGHT_BLUE_FLOWERS)) return GarlandColor.LIGHT_BLUE;
        if (stack.is(ModTags.LIGHT_GRAY_FLOWERS)) return GarlandColor.LIGHT_GRAY;
        if (stack.is(ModTags.CYAN_FLOWERS)) return GarlandColor.CYAN;
        if (stack.is(ModTags.BLACK_FLOWERS)) return GarlandColor.BLACK;
        if (stack.is(ModTags.GRAY_FLOWERS)) return GarlandColor.GRAY;
        return null;
    }
}
