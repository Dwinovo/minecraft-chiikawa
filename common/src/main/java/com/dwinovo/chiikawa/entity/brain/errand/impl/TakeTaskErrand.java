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
import com.dwinovo.chiikawa.entity.brain.errand.step.ClaimSlip;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.ReserveSlip;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.task.BoardSlips;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Walks to the nearest labor board and takes a slip for the pet's job. A pet carries one
 * slip at a time, and goes back for another once it has been paid.
 *
 * <p>The board is where it goes, so it starts only on a board within the anchor's reach, so a
 * following pet takes a slip on the way, and keeps going while the board stays within the leash.
 * The slip is held for the pet as it sets off, so no other pet heads for the same one, and given back
 * unless the pet took it: the routine's own release, since the hold is made in the first step and kept through
 * the walk. The errand's time limit is what ends a walk that goes on too long, and the settings are checked to
 * keep that within the hold, see {@link #problem}.
 *
 * <p>A pet that could not get its slip, because there was none after all, the board was gone or the way to it
 * was not to be found, or that was taken off the walk to the board, rests from the boards for a while.
 */
public final class TakeTaskErrand implements ErrandType<TakeTaskErrand.Trip, ErrandTunings.TakeTask> {
    /**
     * What it set out for.
     *
     * @param board the labor board's block, which stays where it is
     */
    public record Trip(GlobalPos board) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return Optional.of(board);
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.TAKE_TASK;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.TAKE_TASK;
    }

    @Override
    public Codec<ErrandTunings.TakeTask> tuningCodec() {
        return ErrandTunings.TakeTask.CODEC;
    }

    @Override
    public Offers<Trip> offers(PetView view, ErrandTunings.TakeTask tuning) {
        if (view.slip().isPresent()) {
            return Offers.none(Check.fail("has_slip"));
        }
        return view.offeringBoard()
            .map(board -> Offers.of(Offer.of(new Trip(board))))
            .orElseGet(() -> Offers.none(Check.fail("no_slip")));
    }

    /** Having taken the slip ends the walk to the board, and so does a board with nothing for the pet. */
    @Override
    public Check stillWanted(Trip plan, PetView view, ErrandTunings.TakeTask tuning) {
        if (view.slip().isPresent()) {
            return Check.fail("has_slip");
        }
        return view.offeringBoard().isPresent() ? Check.OK : Check.fail("no_slip");
    }

    @Override
    public Routine routine(Trip plan, ErrandSettings<ErrandTunings.TakeTask> settings) {
        ErrandTunings.TakeTask tuning = settings.tuning();
        return Routine.of(
            Routine.step(() -> new ReserveSlip(plan.board())),
            Routine.step(() -> new WalkTo(new Goal.Place(plan.board().pos()), tuning.speed(), tuning.arriveDistance(),
                tuning.takeDistance(), settings.maxTicks(), tuning.noWayTicks())),
            Routine.step(() -> new ClaimSlip(plan.board()))).releasing(handle -> ReserveSlip.letGo(handle, plan.board()));
    }

    /** The hold on a slip has to outlast the errand that made it, or the pet could arrive to a slip somebody else has. */
    @Override
    public Optional<String> problem(ErrandSettings<ErrandTunings.TakeTask> settings) {
        return settings.maxTicks() > BoardSlips.RESERVATION_TICKS
            ? Optional.of("max_ticks " + settings.maxTicks() + " must not be more than the " + BoardSlips.RESERVATION_TICKS
                + " ticks a slip is held for") : Optional.empty();
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.takeTaskCooldown();
    }
}
