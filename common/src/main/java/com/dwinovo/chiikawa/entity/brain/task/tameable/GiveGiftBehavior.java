package com.dwinovo.chiikawa.entity.brain.task.tameable;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import com.dwinovo.chiikawa.gifting.GiftErrand;
import com.dwinovo.chiikawa.gifting.GiftPool;
import com.dwinovo.chiikawa.gifting.GiftingSettings;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
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
 *
 * <p>The present is drawn from the pet's bag as it sets off, one of the thing, and carried in its
 * paws. If the errand is dropped on the way, because the owner has gone or the pet was told to sit,
 * it goes back in the bag. Once it is handed over the pet thinks of no other for a while.
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
        return GiftErrand.mayBegin(pet);
    }

    @Override
    protected void start(ServerLevel level, AbstractPet pet, long gameTime) {
        given = false;
        if (pet.getPendingGift().isEmpty()) {
            GiftPool.draw(GiftPool.of(pet), pet.getRandom())
                .ifPresent(chosen -> pet.setPendingGift(pet.getBackpack().removeItem(chosen.slot(), 1)));
        }
        walkToOwner(pet);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, AbstractPet pet, long gameTime) {
        return !given && !pet.getPendingGift().isEmpty() && pet.getOwner() instanceof Player;
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
        if (!given && !pet.getPendingGift().isEmpty()) {
            putBack(pet);
        }
        // Over, handed across or put back: the pet picks what to do next now, not at its next
        // turn, which would find the errand still its activity and set off on it again.
        IntentSelector.requestReevaluate(pet);
    }

    /** The errand is off: what was being carried goes back in the bag, or on the ground if the bag is full. */
    private static void putBack(AbstractPet pet) {
        ItemStack left = pet.getBackpack().addItem(pet.getPendingGift());
        if (!left.isEmpty()) {
            pet.spawnAtLocation((ServerLevel) pet.level(), left);
        }
        pet.setPendingGift(ItemStack.EMPTY);
    }

    private void hand(ServerLevel level, AbstractPet pet, Player player) {
        ItemStack gift = pet.getPendingGift();
        // What the inventory takes is taken from the copy, so what it could not is what is dropped.
        ItemStack handed = gift.copy();
        if (!player.getInventory().add(handed)) {
            player.drop(handed, false);
        }
        pet.setPendingGift(ItemStack.EMPTY);
        given = true;
        GiftingSettings.current().ifPresent(settings ->
            pet.getBrain().setMemoryWithExpiry(InitMemory.GIFT_COOLDOWN.get(), Unit.INSTANCE, settings.cooldownTicks()));
        pet.tellOwner(Component.translatable("message.chiikawa.gift.given", pet.getDisplayName(), gift.getDisplayName()));
        pet.getNavigation().stop();
        level.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight() * 0.9, pet.getZ(),
            HAPPY_PARTICLES, 0.35, 0.3, 0.35, 0.0);
        pet.triggerReaction(PetReaction.HAPPY);
        pet.playTameSound();
        PetSpeech.say(pet, VoiceMoment.GIFT);
    }

    private static void walkToOwner(AbstractPet pet) {
        LivingEntity owner = pet.getOwner();
        if (owner != null) {
            BehaviorUtils.setWalkAndLookTargetMemories(pet, owner, SPEED, ARRIVE_DISTANCE);
        }
    }
}
