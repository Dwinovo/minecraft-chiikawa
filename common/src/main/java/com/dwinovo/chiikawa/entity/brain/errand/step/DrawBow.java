package com.dwinovo.chiikawa.entity.brain.errand.step;

import com.dwinovo.chiikawa.anim.state.PetAction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import com.dwinovo.chiikawa.entity.brain.errand.run.Handle;
import com.dwinovo.chiikawa.entity.brain.errand.run.Step;
import com.dwinovo.chiikawa.entity.brain.errand.run.StepStatus;
import com.dwinovo.chiikawa.utils.Utils;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Draws the bow at the foe for a while, the footwork going on all the time: an archer with a zombie in its face is
 * an archer being eaten, so backing off is half of shooting. Done when the bow is drawn, or when it cannot be held
 * any longer, because the foe is gone or out of range or the arrows have run out; {@link Release} looses it.
 *
 * <p>Always interruptible, and then the bow is lowered and nothing is loosed: an archer told to sit mid-draw does
 * not shoot. A draw that comes to its end is left raised for {@link Release}, which lowers it.
 */
public final class DrawBow implements Step {
    private final UUID foe;
    private final Stance stance;
    private final double range;
    private final int drawTicks;
    private long startedAt;
    /** Whether the draw came to its end, which is the bow {@link Release} looses and lowers, and not one lowered here. */
    private boolean drawn;

    /**
     * @param foe the creature the fight is about
     * @param stance where the pet stands
     * @param range how far away the foe may be for the bow to be held on it
     * @param drawTicks how long the bow is drawn before the tick it is loosed in
     */
    public DrawBow(UUID foe, Stance stance, double range, int drawTicks) {
        this.foe = foe;
        this.stance = stance;
        this.range = range;
        this.drawTicks = drawTicks;
    }

    /**
     * Whether a bow can be drawn at this foe now: off cooldown, the foe is the pet's to fight and in range, and
     * there is an arrow to draw.
     */
    static boolean canDraw(AbstractPet pet, LivingEntity target, double range) {
        return !pet.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_COOLING_DOWN) && canHold(pet, target, range);
    }

    /** Whether a bow already drawn can be held on this foe: what {@link #canDraw} asks, but for the cooldown. */
    private static boolean canHold(AbstractPet pet, LivingEntity target, double range) {
        return target.isAlive() && target.distanceTo(pet) <= range && PetTargeting.canTarget(pet, target)
            && !Utils.getArrow(pet).isEmpty();
    }

    @Override
    public void begin(Handle handle) {
        startedAt = handle.now();
        drawn = false;
        handle.action(PetAction.BOW_DRAW);
        handle.startUsingMainHand();
    }

    @Override
    public StepStatus tick(Handle handle) {
        LiveHandle live = LiveHandle.of(handle);
        Optional<LivingEntity> target = Skirmish.hunted(live, foe);
        if (target.isEmpty() || !canHold(live.pet(), target.get(), range)) {
            drawn = true;
            return StepStatus.DONE;
        }
        stance.hold(live, target.get());
        if (handle.now() - startedAt >= drawTicks) {
            drawn = true;
            return StepStatus.DONE;
        }
        return StepStatus.RUNNING;
    }

    @Override
    public void release(Handle handle) {
        if (!drawn) {
            handle.stopUsingMainHand();
        }
    }

    @Override
    public boolean holdsGaze() {
        return true;
    }
}
