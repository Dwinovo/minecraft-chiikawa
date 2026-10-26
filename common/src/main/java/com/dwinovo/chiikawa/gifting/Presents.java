package com.dwinovo.chiikawa.gifting;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The giving of a present: the one place a pet gives its owner what is in its paws, however it comes to. The owner is
 * given it, the pet shows its joy and tells them, and the giving is counted twice: against the pet, which rests from
 * giving for as long as the settings say, and against the owner, who is given only so many in a day, all their pets
 * together, see {@link GiftLedger}.
 */
public final class Presents {
    /** The report the giving makes to the gift errand, as its settings name it: the pet's rest from giving begins. */
    public static final String GIVEN = "given";
    private static final int HAPPY_PARTICLES = 10;

    private Presents() {
    }

    /** Where a present ends up. */
    public enum How {
        /** In the owner's hands where there is room and at their feet where there is not: given either way. */
        HAND,
        /** On the floor where the pet stands by the owner's bed, as a cat leaves it. */
        BY_THE_BED
    }

    /**
     * Gives the owner the present in the pet's paws, if the owner may be given one more today: pets that set off together
     * with a present each are not all given leave to give it. Server-side.
     *
     * @param pet the pet
     * @param owner the owner it is given to
     * @param how where it ends up
     * @return whether it was given: there was a present to give and the owner may be given it
     */
    public static boolean give(AbstractPet pet, Player owner, How how) {
        ItemStack gift = pet.getPendingGift();
        if (gift.isEmpty() || !(pet.level() instanceof ServerLevel level) || GiftLedger.of(level).leftToday(level, owner.getUUID()) <= 0) {
            return false;
        }
        // What the inventory takes is taken from the copy, so what it could not is what is dropped.
        ItemStack handed = gift.copy();
        switch (how) {
            case HAND -> {
                if (!owner.getInventory().add(handed)) {
                    owner.drop(handed, false);
                }
            }
            case BY_THE_BED -> {
                float turn = pet.yBodyRot * Mth.DEG_TO_RAD;
                level.addFreshEntity(new ItemEntity(level, pet.getX() - Mth.sin(turn), pet.getY(), pet.getZ() + Mth.cos(turn), handed));
            }
        }
        pet.setPendingGift(ItemStack.EMPTY);
        GiftLedger.of(level).record(owner.getUUID(), GiftLedger.today(level));
        pet.getMind().report(ErrandIds.GIFT_OWNER, GIVEN);
        pet.tellOwner(Component.translatable(how == How.HAND ? "message.chiikawa.gift.given" : "message.chiikawa.gift.left_by_bed",
            pet.getDisplayName(), gift.getDisplayName()));
        level.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight() * 0.9, pet.getZ(),
            HAPPY_PARTICLES, 0.35, 0.3, 0.35, 0.0);
        pet.triggerReaction(PetReaction.HAPPY);
        pet.playTameSound();
        PetSpeech.say(pet, VoiceMoment.GIFT);
        return true;
    }
}
