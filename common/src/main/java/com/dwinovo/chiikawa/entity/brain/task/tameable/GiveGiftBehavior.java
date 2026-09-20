package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Carries what the pet bought for its owner over to them and hands it across.
 *
 * <p>Into their hands where there is room and at their feet where there is not, but
 * handed over either way: a gift the pet keeps hold of because the owner's inventory was
 * full is a gift nobody ever finds out about.
 */
public class GiveGiftBehavior extends Behavior<AbstractPet> {
    private static final float SPEED = 1.0F;
    /** Close enough to hold something out. */
    private static final double REACH_SQR = 4.0;
    /** How near the walk aims: inside reach, not at the edge of it. */
    private static final int ARRIVE_DISTANCE = 1;
    private static final int HAPPY_PARTICLES = 10;
    /** Long enough to cross a garden; a pet that cannot get there gives up and tries later. */
    private static final int GIVE_UP_TICKS = 2400;

    private boolean given;

    public GiveGiftBehavior() {
        super(ImmutableMap.of(
            MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
            MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
        ), GIVE_UP_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, AbstractPet pet) {
        return !pet.getPendingGift().isEmpty() && pet.getOwner() instanceof Player;
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        given = false;
        walkToOwner(pet);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !given && checkExtraStartConditions(level, pet);
    }

    @Override
    protected void tick(ServerLevel level, AbstractPet pet, long gameTime) {
        LivingEntity owner = pet.getOwner();
        if (!(owner instanceof Player player)) {
            return;
        }
        if (pet.distanceToSqr(player) > REACH_SQR) {
            walkToOwner(pet);
            return;
        }
        hand(level, pet, player);
    }

    @Override
    protected void stop(ServerLevel level, AbstractPet pet, long gameTime) {
        pet.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    private void hand(ServerLevel level, AbstractPet pet, Player player) {
        ItemStack gift = pet.getPendingGift();
        if (!player.getInventory().add(gift.copy())) {
            player.drop(gift.copy(), false);
        }
        pet.setPendingGift(ItemStack.EMPTY);
        given = true;
        pet.getNavigation().stop();
        level.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight() * 0.9, pet.getZ(),
            HAPPY_PARTICLES, 0.35, 0.3, 0.35, 0.0);
        pet.triggerReaction(PetReaction.HAPPY);
        pet.playTameSound();
    }

    private static void walkToOwner(AbstractPet pet) {
        LivingEntity owner = pet.getOwner();
        if (owner != null) {
            BehaviorUtils.setWalkAndLookTargetMemories(pet, owner, SPEED, ARRIVE_DISTANCE);
        }
    }
}
