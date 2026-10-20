package com.dwinovo.chiikawa.entity.brain.intent;

import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.task.PetTask;
import net.minecraft.resources.ResourceLocation;

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
    static IntentRequirement wildOrCarrying(ResourceLocation counter) {
        return ctx -> ctx.ownership() instanceof PetOwnership.Wild
                || ctx.task().map(PetTask::counter).filter(counter::equals).isPresent()
            ? IntentCheck.OK
            : IntentCheck.fail("needs_slip");
    }

    /** Only done at night. */
    IntentRequirement AT_NIGHT = ctx -> ctx.phase() == DayPhase.NIGHT ? IntentCheck.OK : IntentCheck.fail("not_night");

    IntentCheck check(IntentContext ctx);
}
