package it.randomuccello.ghirlande;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class GarlandItem extends Item {
    public GarlandItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        GarlandData.fromStack(stack).ifPresent(data -> {
            tooltip.accept(Component.translatable("tooltip.ghirlande.color." + data.color().id()).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.ghirlande.effect." + data.color().id()).withStyle(ChatFormatting.GREEN));

            if (data.color() == GarlandColor.PINK || data.color() == GarlandColor.LIGHT_GRAY) {
                if (data.charges() > 0) {
                    tooltip.accept(Component.translatable("tooltip.ghirlande.charges", data.charges(), data.color().initialCharges())
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltip.accept(Component.translatable("tooltip.ghirlande.exhausted").withStyle(ChatFormatting.RED));
                }
            }

            if (flag.isAdvanced() && !data.flowers().isEmpty()) {
                tooltip.accept(Component.translatable("tooltip.ghirlande.flowers").withStyle(ChatFormatting.DARK_GRAY));
                for (String flower : data.flowers()) {
                    tooltip.accept(Component.literal("  " + flower).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        });
    }
}
