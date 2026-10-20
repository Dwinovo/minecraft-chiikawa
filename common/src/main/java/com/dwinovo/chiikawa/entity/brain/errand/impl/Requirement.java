package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.constraint.PetOwnership;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.task.PetTask;
import net.minecraft.resources.ResourceLocation;

/**
 * An extra start condition an errand is built with, checked before its own.
 */
@FunctionalInterface
public interface Requirement {
    /**
     * A wild pet does this on its own; an owned pet only while it carries a slip that
     * counts the work, since on its own it would strip the grass and mushrooms its owner
     * keeps around the base.
     *
     * @param counter the work counter of the errand's work
     * @return the requirement
     */
    static Requirement wildOrCarrying(ResourceLocation counter) {
        return view -> view.ownership() instanceof PetOwnership.Wild
                || view.slip().map(PetTask::counter).filter(counter::equals).isPresent()
            ? Check.OK
            : Check.fail("needs_slip");
    }

    /** Only done at night. */
    Requirement AT_NIGHT = view -> view.phase() == DayPhase.NIGHT ? Check.OK : Check.fail("not_night");

    Check check(PetView view);
}
