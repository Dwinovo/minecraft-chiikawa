package com.dwinovo.chiikawa.entity.interact;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetMode;
import com.dwinovo.chiikawa.init.InitTag;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public final class PetInteractHandler {
    private static final float TAME_CHANCE = 0.3F;
    private static final float FEED_HEAL = 4.0F;

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
        if (isTame && isOwner && isFood) {
            return handleFeed(level, pet, player, hand);
        }
        if (isTame && isOwner && isSneaking) {
            return handleModeChange(level, pet);
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
                level.broadcastEntityEvent(pet, (byte) 7);
            }
            else {
                level.broadcastEntityEvent(pet, (byte) 6);
            }
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
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private static InteractionResult handleModeChange(Level level, AbstractPet pet) {
        if (!level.isClientSide()) {
            PetMode next = pet.getPetMode().next();
            pet.setPetMode(next);
            if (next == PetMode.WORK) {
                pet.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), pet.blockPosition()));
            }
            else if (next == PetMode.FOLLOW) {
                pet.getBrain().eraseMemory(MemoryModuleType.HOME);
            }
            if (pet.getOwner() instanceof Player owner) {
                owner.sendOverlayMessage(next.getMessage(pet));
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
