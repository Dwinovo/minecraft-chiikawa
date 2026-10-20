package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitMemory;
import java.util.Set;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * A wild pet that heard a whistle goes over to see who blew it, and looks at them until
 * its curiosity runs out. It is never tamed by it: when the memory expires the pet goes
 * back to what it was doing.
 *
 * <p>Its score is above foraging and below taking a slip, all of one priority: a wild pet is
 * curious, but not so curious that it lets a job go.
 */
public final class AnswerWhistleErrand extends LegacyErrand<Plan, ErrandTunings.AnswerWhistle> {
    private static final Heading TO_BLOWER = new Heading(PetView::whistleHeard);

    public AnswerWhistleErrand() {
        super(ErrandIds.ANSWER_WHISTLE, ErrandCategory.CURIOUS, ErrandTunings.AnswerWhistle.CODEC,
            () -> InitActivity.ANSWER_WHISTLE.get());
    }

    @Override
    protected Check canStart(PetView view, ErrandTunings.AnswerWhistle tuning) {
        return view.whistleHeard().isPresent() ? Check.OK : Check.fail("no_whistle");
    }

    @Override
    protected Plan plan(PetView view, ErrandTunings.AnswerWhistle tuning) {
        return TO_BLOWER;
    }

    @Override
    public Set<MemoryModuleType<?>> forgetWhenUnavailable() {
        return Set.of(InitMemory.WHISTLE_HEARD.get());
    }
}
