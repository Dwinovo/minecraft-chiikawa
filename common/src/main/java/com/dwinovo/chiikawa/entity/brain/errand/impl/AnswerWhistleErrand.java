package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandCategory;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandIds;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandType;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.LookAt;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.whistle.WhistleHeard;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * A wild pet that heard a whistle goes over to see who blew it, and looks at them until its curiosity runs
 * out. It is never tamed by it: when the curiosity is gone the pet goes back to what it was doing, with
 * nothing to rest from, since there was nothing wrong with it.
 *
 * <p>Its score is above foraging and below taking a slip, all of one priority: a wild pet is
 * curious, but not so curious that it lets a job go. The whistle is the pet's memory of it, noted by the
 * mind that was told of it, and the walk and the gaze go by that memory each time, so a pet that is whistled to
 * again goes to the new spot, and one that has lost interest on the way stops where it is. The walk has no
 * limit of its own and none for a pet that cannot find a way, as before: the curiosity running out is what ends it.
 */
public final class AnswerWhistleErrand implements ErrandType<AnswerWhistleErrand.Curious, ErrandTunings.AnswerWhistle> {
    /** The whistle the pet has set off for, wherever it was blown: asked of the pet's view each time. */
    public record Curious() implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.whistleHeard();
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.ANSWER_WHISTLE;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.CURIOUS;
    }

    @Override
    public Codec<ErrandTunings.AnswerWhistle> tuningCodec() {
        return ErrandTunings.AnswerWhistle.CODEC;
    }

    @Override
    public Offers<Curious> offers(PetView view, ErrandTunings.AnswerWhistle tuning) {
        return view.whistleHeard().isPresent() ? Offers.of(Offer.of(new Curious())) : Offers.none(Check.fail("no_whistle"));
    }

    /** Goes over to within a few blocks of where it was blown, and looks at whoever is there until it is no longer curious. */
    @Override
    public Routine routine(Curious plan, ErrandSettings<ErrandTunings.AnswerWhistle> settings) {
        ErrandTunings.AnswerWhistle tuning = settings.tuning();
        Goal blower = new Goal.Whistler(tuning.lookRange());
        return Routine.of(
            Routine.step(() -> new WalkTo(blower, tuning.speed(), tuning.closeEnough(), tuning.arriveDistance(),
                settings.maxTicks(), settings.maxTicks())),
            Routine.step(() -> LookAt.untilGone(blower)));
    }

    @Override
    public Set<MemoryModuleType<?>> forgetWhenUnavailable() {
        return Set.of(WhistleHeard.type());
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
