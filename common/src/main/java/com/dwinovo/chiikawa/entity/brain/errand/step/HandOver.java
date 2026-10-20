package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Gives the owner the present in the pet's paws: into their hands where there is room and at their feet where there
 * is not, but given either way, since a present the pet keeps hold of because the owner's inventory was full is a
 * present nobody ever finds out about. The pet tells the owner, shows its joy and says something. Gives up if the
 * owner is gone or the paws are empty. Done in the tick it takes, so it is never cut half way: the present is
 * given or still in the paws, to be put back.
 */
public final class HandOver implements Step {
    private static final int HAPPY_PARTICLES = 10;

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet pet = live.pet();
        Player owner = live.owner().orElse(null);
        ItemStack gift = pet.getPendingGift();
        if (owner == null || gift.isEmpty()) {
            return StepStatus.gaveUp("no_gift");
        }
        // What the inventory takes is taken from the copy, so what it could not is what is dropped.
        ItemStack handed = gift.copy();
        if (!owner.getInventory().add(handed)) {
            owner.drop(handed, false);
        }
        pet.setPendingGift(ItemStack.EMPTY);
        handle.tellOwner(Component.translatable("message.chiikawa.gift.given", pet.getDisplayName(), gift.getDisplayName()));
        pet.getNavigation().stop();
        if (pet.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight() * 0.9, pet.getZ(),
                HAPPY_PARTICLES, 0.35, 0.3, 0.35, 0.0);
        }
        handle.reaction(PetReaction.HAPPY);
        pet.playTameSound();
        handle.say(VoiceMoment.GIFT);
        return StepStatus.DONE;
    }
}
