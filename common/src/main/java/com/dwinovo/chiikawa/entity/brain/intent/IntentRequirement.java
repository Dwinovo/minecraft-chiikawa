package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.task.PetTask;
import net.minecraft.resources.Identifier;

/**
 * An extra start condition an intent is built with, checked before its own.
 */
@FunctionalInterface
public interface IntentRequirement {
    /**
     * A wild pet does this on its own; an owned pet only while it carries a slip that
     * counts the work, since on its own it would strip the grass and mushrooms its owner
     * keeps around the base.
     *
     * @param counter the work counter of the intent's work
     * @return the requirement
     */
    static IntentRequirement wildOrCarrying(Identifier counter) {
        return ctx -> ctx.ownership() instanceof PetOwnership.Wild
                || ctx.task().map(PetTask::counter).filter(counter::equals).isPresent()
            ? IntentCheck.OK
            : IntentCheck.fail("needs_slip");
    }

    /** Only done at night. */
    IntentRequirement AT_NIGHT = ctx -> ctx.phase() == DayPhase.NIGHT ? IntentCheck.OK : IntentCheck.fail("not_night");

    IntentCheck check(IntentContext ctx);

    /**
     * Makes an intent strictly lower in priority than {@code other}: it can only start
     * or continue while {@code other} cannot start. The two never compete on score, so
     * no personality multiplier or randomness can reorder them.
     *
     * <p>{@code other} must be offered and permitted whenever the yielding intent is,
     * i.e. come from the same capability and category.
     *
     * @param other the intent that goes first
     * @return the requirement
     */
    static IntentRequirement yieldsTo(PetIntent other) {
        return ctx -> other.canRun(ctx).ok() ? IntentCheck.fail("higher_priority") : IntentCheck.OK;
    }
}
