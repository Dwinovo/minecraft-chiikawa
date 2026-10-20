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
import com.dwinovo.chiikawa.entity.brain.errand.step.HearResults;
import com.dwinovo.chiikawa.entity.brain.errand.step.Read;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Goes back to the exam desk it sat an exam at, the morning after, to see how it did: walks to its chair, reads the
 * results laid on the desk a moment, and hears them, every result it has out. A pet that cannot get there by noon
 * hears its results wherever it is instead, and one that is on its way to a desk is not told them on the way; see
 * {@link com.dwinovo.chiikawa.qualification.PetExams#upkeep}.
 *
 * <p>Its priority is the exam's: nothing else is on its mind that morning. One that could not get to the desk, or
 * whose desk was broken, rests for a minute before it tries again, which is as good as for the rest of the morning
 * to a pet that will hear the results by noon anyway; one called away rests not at all.
 */
public final class CheckResultsErrand implements ErrandType<CheckResultsErrand.Visit, ErrandTunings.CheckResults> {
    /** The brain's own mover takes the pet right up to the chair; the walk is done when the pet is near enough to read. */
    private static final int RIGHT_UP_TO_IT = 0;

    /**
     * Where the pet has set out to read how it did.
     *
     * @param desk the desk it sat at, which stays where it is until it is broken
     */
    public record Visit(GlobalPos desk) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return Optional.of(desk);
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.CHECK_RESULTS;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.EXAM;
    }

    @Override
    public Codec<ErrandTunings.CheckResults> tuningCodec() {
        return ErrandTunings.CheckResults.CODEC;
    }

    @Override
    public Offers<Visit> offers(PetView view, ErrandTunings.CheckResults tuning) {
        return view.resultsDesk()
            .map(desk -> Offers.of(Offer.of(new Visit(desk))))
            .orElseGet(() -> Offers.none(Check.fail("no_desk")));
    }

    @Override
    public Check stillWanted(Visit plan, PetView view, ErrandTunings.CheckResults tuning) {
        return view.resultsDesk().isPresent() ? Check.OK : Check.fail("no_desk");
    }

    /** The walk lasts as long as the errand may, unless the pet finds no way to the chair. */
    @Override
    public Routine routine(Visit plan, ErrandSettings<ErrandTunings.CheckResults> settings) {
        ErrandTunings.CheckResults tuning = settings.tuning();
        return Routine.of(
            Routine.step(() -> new WalkTo(new Goal.Chair(plan.desk()), tuning.speed(), RIGHT_UP_TO_IT,
                tuning.readingDistance(), settings.maxTicks(), tuning.noWayTicks())),
            Routine.step(() -> new Read(plan.desk(), tuning.readingTicks())),
            Routine.step(HearResults::new));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
