package it.randomuccello.ghirlande;

import java.util.List;
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

    private static final Identifier WOLF_ID = Identifier.fromNamespaceAndPath("minecraft", "wolf");
    private static final Identifier CAT_ID = Identifier.fromNamespaceAndPath("minecraft", "cat");
    private static final Identifier PARROT_ID = Identifier.fromNamespaceAndPath("minecraft", "parrot");
    private static final Identifier NAUTILUS_ID = Identifier.fromNamespaceAndPath("minecraft", "nautilus");
    private static final Identifier ZOMBIE_NAUTILUS_ID = Identifier.fromNamespaceAndPath("minecraft", "zombie_nautilus");

    private static final AttributeModifier RED_HEALTH =
            new AttributeModifier(RED_HEALTH_ID, 4.0D, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier WHITE_KNOCKBACK =
            new AttributeModifier(WHITE_KNOCKBACK_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE);

    private static final List<Holder<MobEffect>> GARLAND_PASSIVE_EFFECTS = List.of(
            MobEffects.SPEED,
            MobEffects.JUMP_BOOST,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.HASTE,
            MobEffects.RESISTANCE,
            MobEffects.NIGHT_VISION,
            MobEffects.STRENGTH
    );

    private static final Map<EntityType<?>, Boolean> HAS_BREEDING_FOOD = new ConcurrentHashMap<>();

    private GarlandGameplay() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateRedHealth(player);
                updateWhiteKnockback(player);
                updatePassiveEffects(player);
                if (player.tickCount % 10 == 0) {
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

            boolean breedingRequested = player.isSecondaryUseActive();
            if (tryPinkInteraction(serverPlayer, animal, breedingRequested)) {
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

    private static void updatePassiveEffects(ServerPlayer player) {
        GarlandColor color = equippedGarland(player)
                .map(GarlandData::color)
                .orElse(GarlandColor.MIXED);
        Holder<MobEffect> desired = passiveEffectFor(color);

        for (Holder<MobEffect> effect : GARLAND_PASSIVE_EFFECTS) {
            if (effect.equals(desired)) {
                ensureInfiniteGarlandEffect(player, effect);
            } else {
                removeInfiniteGarlandEffect(player, effect);
            }
        }
    }

    private static Holder<MobEffect> passiveEffectFor(GarlandColor color) {
        return switch (color) {
            case YELLOW -> MobEffects.SPEED;
            case BLUE -> MobEffects.JUMP_BOOST;
            case ORANGE -> MobEffects.FIRE_RESISTANCE;
            case MAGENTA -> MobEffects.HASTE;
            case GRAY -> MobEffects.RESISTANCE;
            case CYAN -> MobEffects.NIGHT_VISION;
            case BLACK -> MobEffects.STRENGTH;
            default -> null;
        };
    }

    private static void ensureInfiniteGarlandEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null) {
            player.addEffect(new MobEffectInstance(
                    effect,
                    MobEffectInstance.INFINITE_DURATION,
                    0,
                    true,
                    false,
                    true
            ));
        }
    }

    private static void removeInfiniteGarlandEffect(ServerPlayer player, Holder<MobEffect> effect) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null
                && current.getAmplifier() == 0
                && current.isInfiniteDuration()
                && current.isAmbient()) {
            player.removeEffect(effect);
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

    private static boolean tryPinkInteraction(ServerPlayer player, Animal animal, boolean breedingRequested) {
        if (breedingRequested) {
            if (animal.isBaby()) {
                return false;
            }

            if (canHealAfterBreeding(animal)) {
                animal.heal(2.0F);
                return true;
            }

            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());
            if (PARROT_ID.equals(typeId) || ZOMBIE_NAUTILUS_ID.equals(typeId)) {
                return false;
            }

            if (animal instanceof TamableAnimal tameable && !tameable.isTame()) {
                return false;
            }
            if (animal instanceof AbstractHorse horse && !horse.isTamed()) {
                return false;
            }

            if (hasVanillaBreedingFood(animal) && animal.canFallInLove()) {
                animal.setInLove(player);
                return true;
            }
            return false;
        }

        if (tryTamingAttempt(player, animal)) {
            return true;
        }

        if (canHealAfterBreeding(animal)) {
            animal.heal(2.0F);
            return true;
        }

        return false;
    }

    private static boolean tryTamingAttempt(ServerPlayer player, Animal animal) {
        if (animal instanceof AbstractHorse horse && !horse.isTamed()) {
            if (horse.isBaby()) {
                return false;
            }

            int maxTemper = horse.getMaxTemper();
            boolean tamed = maxTemper > 0 && horse.getRandom().nextInt(maxTemper) < horse.getTemper();
            if (tamed) {
                horse.tameWithName(player);
                horse.level().broadcastEntityEvent(horse, (byte) 7);
            } else {
                horse.modifyTemper(5);
                horse.level().broadcastEntityEvent(horse, (byte) 6);
            }
            return true;
        }

        if (animal instanceof TamableAnimal tameable && !tameable.isTame()) {
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType());
            int denominator = tamingChanceDenominator(typeId);
            if (denominator <= 0) {
                return false;
            }

            boolean tamed = tameable.getRandom().nextInt(denominator) == 0;
            if (tamed) {
                tameable.tame(player);
                tameable.getNavigation().stop();
                tameable.level().broadcastEntityEvent(tameable, (byte) 7);
            } else {
                tameable.level().broadcastEntityEvent(tameable, (byte) 6);
            }
            return true;
        }

        return false;
    }

    private static int tamingChanceDenominator(Identifier typeId) {
        if (PARROT_ID.equals(typeId)) {
            return 10;
        }
        if (WOLF_ID.equals(typeId)
                || CAT_ID.equals(typeId)
                || NAUTILUS_ID.equals(typeId)
                || ZOMBIE_NAUTILUS_ID.equals(typeId)) {
            return 3;
        }
        return -1;
    }

    private static boolean canHealAfterBreeding(Animal animal) {
        return (animal.isInLove() || animal.getAge() > 0)
                && animal.getHealth() < animal.getMaxHealth();
    }

    private static boolean hasVanillaBreedingFood(Animal animal) {
        return HAS_BREEDING_FOOD.computeIfAbsent(animal.getType(), ignored ->
                BuiltInRegistries.ITEM.stream().anyMatch(item -> animal.isFood(item.getDefaultInstance())));
    }
}
