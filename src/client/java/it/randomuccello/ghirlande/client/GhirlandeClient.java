package it.randomuccello.ghirlande.client;

import com.mojang.blaze3d.platform.InputConstants;
import it.randomuccello.ghirlande.GarlandData;
import it.randomuccello.ghirlande.GhirlandeMod;
import it.randomuccello.ghirlande.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GhirlandeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpecialModelRenderers.ID_MAPPER.put(GhirlandeMod.id("garland"), GarlandSpecialRenderer.Unbaked.MAP_CODEC);

        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipFlag, lines) -> {
            if (!stack.is(ModItems.GARLAND)) {
                return;
            }

            GarlandData.fromStack(stack).ifPresent(data -> {
                if (data.flowers().isEmpty()) {
                    return;
                }

                if (!isShiftDown()) {
                    lines.add(Component.translatable("tooltip.ghirlande.hold_shift")
                            .withStyle(ChatFormatting.DARK_GRAY));
                    return;
                }

                lines.add(Component.translatable("tooltip.ghirlande.flowers")
                        .withStyle(ChatFormatting.DARK_GRAY));
                for (String rawId : data.flowers()) {
                    Identifier id = Identifier.tryParse(rawId);
                    if (id == null) {
                        continue;
                    }

                    Item item = BuiltInRegistries.ITEM.getValue(id);
                    if (item == null || item == Items.AIR) {
                        continue;
                    }

                    Component flowerName = new ItemStack(item).getHoverName();
                    lines.add(Component.literal("  ").append(flowerName)
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            });
        });
    }

    private static boolean isShiftDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }
}
