package it.randomuccello.ghirlande;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

public final class GarlandGameplay {
    private static final Identifier RED_HEALTH_ID = GhirlandeMod.id("red_garland_health");
    private static final Identifier WHITE_KNOCKBACK_ID = GhirlandeMod.id("white_garland_knockback");
    private static final Identifier PARROT_ID = Identifier.fromNamespaceAndPath("minecraft", "parrot");
    private static final Identifier ZOMBIE_NAUTILUS_ID = Identifier.fromNamespaceAndPath("minecraft", "zombie_nautilus");

    private static final AttributeModifier RED_HEALTH =
            new AttributeModifier(RED_HEALTH_ID, 4.0D, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier WHITE_KNOCKBACK =
            new AttributeModifier(WHITE_KNOCKBACK_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE);

    private static final Map<EntityType<?>, Boolean> HAS_BREEDING_FOOD = new ConcurrentHashMap<>();

    private GarlandGameplay() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateRedHealth(player);
                updateWhiteKnockback(player);
                if (player.tickCount % 10 == 0) {
                    updatePassiveEffect(player);
                    ensureRespiration(player);
                }
            }
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() || hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer serverPlayer) || !(entity instanceof Animal animal)) {
                return InteractionResult.PASS;
            }

            ItemStack garland = equippedGarlandStack(serverPlayer);
            Optional<GarlandData> data = GarlandData.fromStack(garland);
            if (data.isEmpty() || data.get().color() != GarlandColor.PINK || data.get().charges() <= 0) {
                return InteractionResult.PASS;
            }

            if (tryPinkInteraction(serverPlayer, animal)) {
                GarlandData.consumeCharge(garland);
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            if (!player.isSecondaryUseActive() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            ItemStack garland = equippedGarlandStack(serverPlayer);
            Optional<GarlandData> data = GarlandData.fromStack(garland);
            if (data.isEmpty() || data.get().color() != GarlandColor.LIGHT_GRAY || data.get().charges() <= 0) {
                return InteractionResult.PASS;
            }

            ItemStack virtualBoneMeal = new ItemStack(Items.BONE_MEAL);
            var pos = hitResult.getBlockPos();
            var face = hitResult.getDirection();
            boolean used = BoneMealItem.growCrop(virtualBoneMeal, level, pos);
            var particlePos = pos;

            if (!used) {
                BlockState clickedState = level.getBlockState(pos);
                var relative = pos.relative(face);
                if (clickedState.isFaceSturdy(level, pos, face)
                        && BoneMealItem.growWaterPlant(virtualBoneMeal, level, relative, face)) {
                    used = true;
                    particlePos = relative;
                }
            }

            if (used) {
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, particlePos, 15);
                GarlandData.consumeCharge(garland);
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.PASS;
        });
    }

    public static Optional<GarlandData> equippedGarland(ServerPlayer player) {
        return GarlandData.fromStack(equippedGarlandStack(player));
    }

    public static ItemStack equippedGarlandStack(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(ModItems.GARLAND) ? head : ItemStack.EMPTY;
    }

    private static void updateRedHealth(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

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

    private static void updateWhiteKnockback(ServerPlayer player) {
        AttributeInstance attackKnockback = player.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (attackKnockback == null) return;

        boolean shouldHave = player.getMainHandItem().isEmpty()
                && equippedGarland(player).map(data -> data.color() == GarlandColor.WHITE).orElse(false);

        if (shouldHave) {
            attackKnockback.addOrUpdateTransientModifier(WHITE_KNOCKBACK);
        } else if (attackKnockback.hasModifier(WHITE_KNOCKBACK_ID)) {
            attackKnockback.removeModifier(WHITE_KNOCKBACK_ID);
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
            case GRAY -> refresh(player, MobEffects.RESISTANCE);
            case CYAN -> refresh(player, MobEffects.NIGHT_VISION);
            case BLACK -> refresh(player, MobEffects.STRENGTH);
            default -> {
            }
        }
    }

    private static void ensureRespiration(ServerPlayer player) {
        ItemStack stack = equippedGarlandStack(player);
        Optional<GarlandData> data = GarlandData.fromStack(stack);
        if (data.isEmpty() || data.get().color() != GarlandColor.LIGHT_BLUE) {
            return;
        }

        Holder<Enchantment> respiration = player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.RESPIRATION);
        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (current.getLevel(respiration) < 1) {
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(respiration, 1));
        }
    }

    private static void refresh(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.getAmplifier() > 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, 30, 0, true, false, true));
    }

    private static boolean tryPinkInteraction(ServerPlayer player, Animal animal) {
        if ((animal.isInLove() || animal.getAge() > 0) && animal.getHealth() < animal.getMaxHealth()) {
            animal.heal(2.0F);
            return true;
        }

        if (animal instanceof TamableAnimal tameable && !tameable.isTame()) {
            tameable.tame(player);
            return true;
        }

        if (animal instanceof AbstractHorse horse && !horse.isTamed()) {
            return horse.tameWithName(player);
        }

        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());
        if (PARROT_ID.equals(typeId) || ZOMBIE_NAUTILUS_ID.equals(typeId)) {
            return false;
        }

        if (hasVanillaBreedingFood(animal) && animal.canFallInLove()) {
            animal.setInLove(player);
            return true;
        }

        return false;
    }

    private static boolean hasVanillaBreedingFood(Animal animal) {
        return HAS_BREEDING_FOOD.computeIfAbsent(animal.getType(), ignored ->
                BuiltInRegistries.ITEM.stream().anyMatch(item -> animal.isFood(item.getDefaultInstance())));
    }
}
