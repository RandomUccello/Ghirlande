package it.randomuccello.ghirlande;

import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public final class GarlandGameplay {
    private static final Identifier RED_HEALTH_ID = GhirlandeMod.id("red_garland_health");
    private static final AttributeModifier RED_HEALTH =
            new AttributeModifier(RED_HEALTH_ID, 4.0D, AttributeModifier.Operation.ADD_VALUE);

    private GarlandGameplay() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateRedHealth(player);
                if (player.tickCount % 10 == 0) {
                    updatePassiveEffect(player);
                }
            }
        });
    }

    public static Optional<GarlandData> equippedGarland(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(ModItems.GARLAND)) {
            return Optional.empty();
        }
        return GarlandData.fromStack(head);
    }

    private static void updateRedHealth(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        boolean shouldHave = equippedGarland(player)
                .map(data -> data.color() == GarlandColor.RED)
                .orElse(false);

        if (shouldHave) {
            maxHealth.addOrUpdateTransientModifier(RED_HEALTH);
        } else if (maxHealth.hasModifier(RED_HEALTH_ID)) {
            maxHealth.removeModifier(RED_HEALTH_ID);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void updatePassiveEffect(ServerPlayer player) {
        GarlandColor color = equippedGarland(player)
                .map(GarlandData::color)
                .orElse(GarlandColor.MIXED);

        switch (color) {
            case YELLOW -> refresh(player, MobEffects.SPEED);
            case BLUE -> refresh(player, MobEffects.JUMP_BOOST);
            case ORANGE -> refresh(player, MobEffects.FIRE_RESISTANCE);
            case MAGENTA -> refresh(player, MobEffects.HASTE);
            case LIGHT_GRAY -> refresh(player, MobEffects.RESISTANCE);
            case CYAN -> refresh(player, MobEffects.NIGHT_VISION);
            case BLACK -> refresh(player, MobEffects.STRENGTH);
            default -> {
            }
        }
    }

    private static void refresh(ServerPlayer player, net.minecraft.core.Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        // Do not replace a stronger external potion/beacon effect.
        if (current != null && current.getAmplifier() > 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, 30, 0, true, false, true));
    }
}
