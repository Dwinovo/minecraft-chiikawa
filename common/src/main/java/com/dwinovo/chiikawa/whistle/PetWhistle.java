package com.dwinovo.chiikawa.whistle;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetDirective;
import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.intent.IntentSelector;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.dwinovo.chiikawa.whistle.WhistleHearing.Hearing;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Blowing a whistle for the pets near the blower, and the pets answering. Knows nothing
 * about the item that was blown: the order and how a whistle behaves are handed in.
 *
 * <p>The blower's own pets each take the order after a wait of their own, held as a
 * {@link WhistleCall} until {@link #hear} finds it due. Wild pets that hear a call come
 * over to look for a while, held as a {@link WhistleHeard} that an intent answers; they
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

    /** Gives every pet within earshot of {@code owner} the order, or its curiosity. */
    public static Heard blow(ServerPlayer owner, PetDirective order, WhistleSettings settings) {
        ServerLevel level = owner.serverLevel();
        long now = level.getGameTime();
        GlobalPos where = GlobalPos.of(level.dimension(), owner.blockPosition());
        int owned = 0;
        int wild = 0;
        for (AbstractPet pet : level.getEntitiesOfClass(AbstractPet.class,
                owner.getBoundingBox().inflate(settings.range()), AbstractPet::isAlive)) {
            Hearing hearing = WhistleHearing.of(owner.getUUID(), order, PetOwnership.of(pet),
                pet.level() == level, pet.distanceToSqr(owner), settings.range(),
                pet.getActivity() == PetActivity.EXAM);
            long at = now + pet.getRandom().nextInt(settings.reactDelayTicks() + 1);
            switch (hearing) {
                case OWNED -> {
                    pet.getBrain().setMemory(InitMemory.WHISTLE_CALL.get(), new WhistleCall(order, at));
                    owned++;
                }
                case WILD -> {
                    // The expiry counts from now, so the wait is part of how long it is interested.
                    pet.getBrain().setMemoryWithExpiry(InitMemory.WHISTLE_HEARD.get(), new WhistleHeard(where, at),
                        at - now + settings.curiousTicks());
                    IntentSelector.requestReevaluate(pet);
                    wild++;
                }
                case DEAF -> {
                }
            }
        }
        return new Heard(owned, wild);
    }

    /**
     * Takes the order the pet was whistled, once its wait is over. Called every tick from
     * the pet's own server step, so it only reads a memory until there is something to do.
     */
    public static void hear(AbstractPet pet) {
        Optional<WhistleCall> call = pet.getBrain().getMemory(InitMemory.WHISTLE_CALL.get());
        if (call.isEmpty() || pet.level().getGameTime() < call.get().at()) {
            return;
        }
        pet.getBrain().eraseMemory(InitMemory.WHISTLE_CALL.get());
        // A pet let go of since the blow has no order left to take.
        if (!(PetOwnership.of(pet) instanceof PetOwnership.Owned)) {
            return;
        }
        pet.setPetDirective(call.get().order());
        if (call.get().order() == PetDirective.FOLLOW) {
            pet.triggerReaction(PetReaction.HAPPY);
            PetSpeech.say(pet, VoiceMoment.WHISTLE);
        }
    }
}
