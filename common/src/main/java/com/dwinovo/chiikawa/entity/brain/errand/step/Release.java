package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import java.util.UUID;

/**
 * Looses the bow that {@link DrawBow} drew: an arrow at the foe, if it is still the pet's to shoot, then the bow is
 * lowered and held off for as long as the cooldown says. The arrow's power, spread and cost are the pet's own, see
 * {@link AbstractPet#performRangedAttack}.
 *
 * <p>An action of one tick, so it is never cut half way: an errand asked to end waits for the next tick at most, and
 * ends either before the arrow flies or after. Either way the bow comes down, so a pet told to sit as its bow comes to
 * its full draw does not shoot and is not left holding it up.
 */
public final class Release implements Step {
    /** Fully drawn: what the pet's shot makes of it is its own affair. */
    private static final float FULL_DRAW = 1.0F;

    private final UUID foe;
    private final int cooldownTicks;

    /**
     * @param foe the creature the fight is about
     * @param cooldownTicks how long the pet holds off its next attack
     */
    public Release(UUID foe, int cooldownTicks) {
        this.foe = foe;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public void begin(Handle handle) {
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        AbstractPet pet = live.pet();
        // Only at a foe it is still allowed to shoot.
        Skirmish.hunted(live, foe).filter(target -> PetTargeting.canTarget(pet, target))
            .ifPresent(target -> pet.performRangedAttack(target, FULL_DRAW));
        handle.cooldownAttack(cooldownTicks);
        return StepStatus.DONE;
    }

    @Override
    public void release(Handle handle) {
        handle.stopUsingMainHand();
    }
}
