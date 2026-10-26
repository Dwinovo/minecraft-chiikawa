package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.Offer;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.Plan;
import com.dwinovo.chiikawa.entity.brain.errand.rest.SubjectRests;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * What the pet has set out to work: the block the work sensor chose for the errand, which is where the pet goes and
 * what a rest is on, so that a crop it could not reach is left alone for a while and not the other crops.
 *
 * <p>The block is where it goes, so the errand starts only on a block within the anchor's reach and keeps going while
 * the block stays within the leash. It keeps to the block it set off for: if the sensor moves on to another because that
 * one is no longer worth working, the errand is over.
 *
 * @param where the block
 */
public record Site(GlobalPos where) implements Plan {
    /** The brain's own mover takes the pet right up to the block; the walk is done when the pet is near enough to work it. */
    private static final int RIGHT_UP_TO_IT = 0;

    @Override
    public Optional<GlobalPos> destination(PetView view) {
        return Optional.of(where);
    }

    @Override
    public Optional<String> subject() {
        return Optional.of(SubjectRests.subject(where.pos()));
    }

    /** @return setting off for the block the sensor chose for the errand, or why there is none: the target's {@code missing} */
    static Offers<Site> offers(Work<Site, ?> work, ResourceLocation errand, PetView view) {
        return view.targets().work(errand)
            .map(where -> Offers.of(Offer.of(new Site(where))))
            .orElseGet(() -> Offers.none(Check.fail(work.targetOf(WorkTarget.BlockTarget.class).missing())));
    }

    /** @return whether the sensor still chooses the block the pet set off for, or why not: the target's {@code missing} */
    Check stillChosen(Work<Site, ?> work, ResourceLocation errand, PetView view) {
        return view.targets().work(errand).filter(where::equals).isPresent() ? Check.OK
            : Check.fail(work.targetOf(WorkTarget.BlockTarget.class).missing());
    }

    /** @return the walk to the block, which is done once the pet is near enough to work it */
    static Routine.Part walk(BlockPos block, float speed, double reachSqr, int maxTicks, int noWayTicks) {
        return Routine.step(() -> new WalkTo(new Goal.Place(block), speed, RIGHT_UP_TO_IT, Math.sqrt(reachSqr), maxTicks,
            noWayTicks));
    }
}
