package com.dwinovo.chiikawa.whistle;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.PetMind;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.inbox.Message;
import com.dwinovo.chiikawa.qualification.PetExams;
import com.dwinovo.chiikawa.whistle.WhistleHearing.Hearing;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Blowing a whistle for the pets near the blower. Knows nothing about the item that was blown: the order
 * and how a whistle behaves are handed in, and who hears it is {@link WhistleHearing}'s to say.
 *
 * <p>What a pet makes of the whistle is its own to do: this only tells it. The blower's own pets, and those of their
 * friends, are told the order, and each takes it after a wait of its own, see {@link Message.WhistleOrder}. Wild pets that hear a
 * call are told where it was blown from and for how long it interests them, see {@link Message.WhistleHeard}; they
 * are never tamed by it and their order is never touched.
 */
public final class PetWhistle {
    private PetWhistle() {
    }

    /**
     * @param owned how many of the blower's own pets heard it
     * @param wild how many wild pets heard it and will come over
     */
    public record Heard(int owned, int wild) {
        public boolean nobody() {
            return owned == 0 && wild == 0;
        }
    }

    /** Tells every pet within earshot of {@code owner} the order, or what has caught its curiosity. */
    public static Heard blow(ServerPlayer owner, PetDirective order, WhistleSettings settings) {
        ServerLevel level = owner.serverLevel();
        long now = level.getGameTime();
        GlobalPos where = GlobalPos.of(level.dimension(), owner.blockPosition());
        int owned = 0;
        int wild = 0;
        for (AbstractPet pet : level.getEntitiesOfClass(AbstractPet.class,
                owner.getBoundingBox().inflate(settings.range()), AbstractPet::isAlive)) {
            Hearing hearing = WhistleHearing.of(owner.getUUID(), order, PetOwnership.of(pet),
                pet.isAlliedTo(owner), pet.level() == level, pet.distanceToSqr(owner), settings.range(),
                PetExams.seatedAtExam(pet));
            switch (hearing) {
                case OWNED -> {
                    PetMind.post(pet, new Message.WhistleOrder(order, settings.reactDelayTicks()));
                    owned++;
                }
                case WILD -> {
                    // The wait counts from now, so it is part of how long the pet is interested.
                    long from = now + pet.getRandom().nextInt(settings.reactDelayTicks() + 1);
                    PetMind.post(pet, new Message.WhistleHeard(where, from, from + settings.curiousTicks()));
                    wild++;
                }
                case DEAF -> {
                }
            }
        }
        return new Heard(owned, wild);
    }
}
