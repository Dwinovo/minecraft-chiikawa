package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.anim.state.PetActivity;
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
import com.dwinovo.chiikawa.entity.brain.errand.run.StepTag;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.HandIn;
import com.dwinovo.chiikawa.entity.brain.errand.step.Hold;
import com.dwinovo.chiikawa.entity.brain.errand.step.SitDown;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.step.Write;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Sits a licence exam at the desk its owner signed it up at: that day, until the exam closes. The pet walks to the
 * chair of the desk, sits on it up against the desk facing the way the desk faces, writes for as long as the licence
 * says, and hands the paper in, pushing it across the desk before it gets up. The result is decided as it is
 * handed in and heard the morning after; a pet called away before it hands in has sat nothing, and can come back to
 * it while the exam is on.
 *
 * <p>The desk is where it goes, so it starts only on a desk within the anchor's reach, as a slip is taken, and a pet
 * at heel sits it while its owner stays near the desk. Once it is in its chair it has nowhere left to go, and it
 * is kept at until it gets up: a pet that has just handed its paper in is still in its chair, though it is no
 * longer signed up to anything.
 *
 * <p>Its priority is above taking a slip or getting on with work: the owner paid for this. One that could not get to
 * its chair, or whose desk was broken, rests for ten seconds before it tries again; one told the exam is off, or
 * called away, rests not at all, since nothing was wrong with it.
 */
public final class TakeExamErrand implements ErrandType<TakeExamErrand.Sitting, ErrandTunings.TakeExam> {
    /** The brain's own mover takes the pet right up to the chair; the walk is done when the pet is near enough to sit. */
    private static final int RIGHT_UP_TO_IT = 0;

    /**
     * What the pet has been signed up to sit.
     *
     * @param qualification the licence the exam is for
     * @param desk the desk its owner signed it up at, which stays where it is until it is broken
     */
    public record Sitting(ResourceLocation qualification, GlobalPos desk) implements Plan {
        /** Where the chair is, until the pet is in it. */
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.seatedAtExam() ? Optional.empty() : Optional.of(desk);
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.TAKE_EXAM;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.EXAM;
    }

    @Override
    public Codec<ErrandTunings.TakeExam> tuningCodec() {
        return ErrandTunings.TakeExam.CODEC;
    }

    @Override
    public Offers<Sitting> offers(PetView view, ErrandTunings.TakeExam tuning) {
        return view.examToSit()
            .map(summons -> Offers.of(Offer.of(new Sitting(summons.qualification(), summons.desk()))))
            .orElseGet(() -> Offers.none(Check.fail("no_desk")));
    }

    /** Kept at until it gets up: a pet that has just handed its paper in is still in its chair. */
    @Override
    public Check stillWanted(Sitting plan, PetView view, ErrandTunings.TakeExam tuning) {
        return view.seatedAtExam() || view.examToSit().isPresent() ? Check.OK : Check.fail("no_desk");
    }

    /** The walk lasts as long as the errand may, unless the pet finds no way to the chair. */
    @Override
    public Routine routine(Sitting plan, ErrandSettings<ErrandTunings.TakeExam> settings) {
        ErrandTunings.TakeExam tuning = settings.tuning();
        return Routine.of(
            Routine.step(() -> new WalkTo(new Goal.Chair(plan.desk()), tuning.speed(), RIGHT_UP_TO_IT,
                tuning.arriveDistance(), settings.maxTicks(), tuning.noWayTicks())),
            Routine.step(() -> new SitDown(plan.desk())),
            Routine.step(() -> new Write(plan.desk(), plan.qualification())),
            Routine.step(() -> new HandIn(plan.desk(), plan.qualification())),
            Routine.step(() -> new Hold(tuning.handInTicks(), PetActivity.EXAM, Set.of(StepTag.SEATED), Optional.empty(),
                Hold.TIME_ALONE)));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
