package com.dwinovo.chiikawa.entity.interact;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import com.dwinovo.chiikawa.init.InitItems;
import com.dwinovo.chiikawa.init.InitTag;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.shop.Wallet;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PetInteractHandler {
    private static final float TAME_CHANCE = 0.3F;
    /**
     * What a snack is worth to a pet. Shared with the pet buying its own: a cookie heals
     * the same whether an owner hands it over or the pet queues up for it.
     */
    public static final float FEED_HEAL = 4.0F;
    /** How long a dish keeps a pet in the mood: a Minecraft day's work is 24000. */
    private static final int DISH_TICKS = 6000;

    private PetInteractHandler() {
    }

    public static InteractionResult handle(AbstractPet pet, Player player, InteractionHand hand) {
        Level level = pet.level();
        boolean isTame = pet.isTame();
        boolean isOwner = pet.isOwnedBy(player);
        boolean isSneaking = player.isShiftKeyDown();
        ItemStack held = player.getItemInHand(hand);
        boolean isFood = held.is(InitTag.ENTITY_TAME_FOODS);

        if (!isTame && isFood) {
            return handleTame(level, pet, player, hand);
        }
        if (isTame && isOwner && held.is(InitItems.SIMPLE_DISH.get())) {
            return handleDish(level, pet, player, hand);
        }
        if (isTame && isOwner && Wallet.isMoney(held)) {
            return handleGiveMoney(level, pet, player, hand);
        }
        if (isTame && isOwner && isFood) {
            return handleFeed(level, pet, player, hand);
        }
        if (isTame && isOwner && isSneaking) {
            return handleDirectiveChange(level, pet);
        }
        if (isTame && isOwner && !isSneaking && !isFood) {
            return handleOpenMenu(level, pet, player);
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult handleTame(Level level, AbstractPet pet, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            if (!player.isCreative()) {
                player.getItemInHand(hand).shrink(1);
            }
            if (level.getRandom().nextFloat() < TAME_CHANCE) {
                pet.tame(player);
                pet.playTameSound();
                pet.triggerReaction(PetReaction.HAPPY);
                PetSpeech.say(pet, VoiceMoment.TAME);
                level.broadcastEntityEvent(pet, (byte) 7);
            }
            else {
                pet.triggerReaction(PetReaction.CONFUSED);
                level.broadcastEntityEvent(pet, (byte) 6);
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /**
     * A proper meal rather than a snack: it heals nothing and instead puts the pet in the
     * mood to work for a while. Fed to a pet that is already eager, it tops the mood up
     * rather than stacking — two dishes at once would be a way to keep a pet permanently
     * sprinting, which is not what a meal is.
     */
    private static InteractionResult handleDish(Level level, AbstractPet pet, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
            pet.feedDish(DISH_TICKS);
            pet.triggerReaction(PetReaction.HAPPY);
            pet.playTameSound();
            IntentSelector.requestReevaluate(pet);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /**
     * Spending money is the pet's business; where it comes from need not be. An owner can
     * hand over money and let the pet go and choose something for itself, which is the
     * whole difference between a pet and a vending machine.
     */
    private static InteractionResult handleGiveMoney(Level level, AbstractPet pet, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack held = player.getItemInHand(hand);
            ItemStack given = player.getAbilities().instabuild ? held.copy() : held;
            ItemStack left = pet.getBackpack().addItem(given);
            if (left.getCount() == given.getCount()) {
                // Nowhere to put it: nothing changes hands rather than the coins vanishing.
                return InteractionResult.PASS;
            }
            if (player.getAbilities().instabuild) {
                // The stack in a creative hand is untouched, but what the pet took is real.
                pet.getBackpack().setChanged();
            }
            pet.triggerReaction(PetReaction.HAPPY);
            pet.playTameSound();
            IntentSelector.requestReevaluate(pet);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private static InteractionResult handleFeed(Level level, AbstractPet pet, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            if (!player.isCreative()) {
                player.getItemInHand(hand).shrink(1);
            }
            if (pet.getHealth() < pet.getMaxHealth()) {
                pet.heal(FEED_HEAL);
                pet.triggerReaction(PetReaction.HAPPY);
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private static InteractionResult handleDirectiveChange(Level level, AbstractPet pet) {
        if (!level.isClientSide()) {
            PetDirective next = pet.getPetDirective().next();
            pet.setPetDirective(next);
            if (pet.getOwner() instanceof Player owner) {
                owner.sendOverlayMessage(next.message(pet));
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private static InteractionResult handleOpenMenu(Level level, AbstractPet pet, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new PetBackpackMenu(containerId, inventory, pet),
                Component.translatable("menu.chiikawa.pet_backpack")
            ));
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
}
