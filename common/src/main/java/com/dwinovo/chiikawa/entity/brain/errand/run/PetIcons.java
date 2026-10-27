package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.entity.brain.errand.decide.WhyNot;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import java.util.Optional;
import java.util.Set;

/**
 * Which icon a pet wears, from what it is and what it found: one pure table, so what shows over its head is a function
 * of facts the rest of the game already has and not another thing to keep in step. The first row that holds wins:
 *
 * <table>
 *   <caption>The icons</caption>
 *   <tr><th>Icon</th><th>When</th></tr>
 *   <tr><td>{@link PetIcon#SCARED}</td><td>the step it is at is {@link StepTag#SCARED}</td></tr>
 *   <tr><td>{@link PetIcon#SLEEPY}</td><td>the step it is at is {@link StepTag#SLEEPING}: it is lying down asleep</td></tr>
 *   <tr><td>{@link PetIcon#GIFT}</td><td>it carries a present, or has one in mind that it may set off with</td></tr>
 *   <tr><td>{@link PetIcon#MISSING}</td><td>its work has nothing to offer for want of something, see {@link WhyNot}</td></tr>
 *   <tr><td>{@link PetIcon#SEEKING_WORK}</td><td>its work has nothing to offer, by day</td></tr>
 *   <tr><td>{@link PetIcon#NONE}</td><td>otherwise</td></tr>
 * </table>
 */
public final class PetIcons {
    private PetIcons() {
    }

    /**
     * What the table asks of the pet besides the step it is at.
     *
     * @param carryingPresent whether it has a present for its owner in its paws
     * @param presentReady whether it has a present in mind that it may set off with, and something to give
     * @param whyNot why it is not at work, if it is not
     * @param phase the part of the day
     */
    public record Signs(boolean carryingPresent, boolean presentReady, Optional<WhyNot> whyNot, DayPhase phase) {
        /** A pet with nothing to show, by day. */
        public static final Signs NONE = new Signs(false, false, Optional.empty(), DayPhase.DAY);
    }

    /**
     * @param tags what the step the pet is at makes of it
     * @param signs the rest
     * @return the icon it wears
     */
    public static PetIcon of(Set<StepTag> tags, Signs signs) {
        if (tags.contains(StepTag.SCARED)) {
            return PetIcon.SCARED;
        }
        if (tags.contains(StepTag.SLEEPING)) {
            return PetIcon.SLEEPY;
        }
        if (signs.carryingPresent() || signs.presentReady()) {
            return PetIcon.GIFT;
        }
        if (signs.whyNot().flatMap(WhyNot::lack).isPresent()) {
            return PetIcon.MISSING;
        }
        if (signs.whyNot().isPresent() && signs.phase() != DayPhase.NIGHT) {
            return PetIcon.SEEKING_WORK;
        }
        return PetIcon.NONE;
    }
}
