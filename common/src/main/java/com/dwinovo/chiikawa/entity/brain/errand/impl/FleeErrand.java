package com.dwinovo.chiikawa.entity.brain.errand.impl;

import com.dwinovo.chiikawa.entity.brain.constraint.PetAnchor;
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
import com.dwinovo.chiikawa.entity.brain.errand.step.Cower;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.RunTo;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Gets out of harm's way. A pet that has been hurt by something and will not fight it, because the owner has told it not to
 * or told it to sit, or because it has nothing to fight with, runs to hide behind its owner, or at home, or away, and cowers
 * until nothing has hurt it for a while and what did is far off. It matters more than anything else a pet does, and it is
 * the one errand that no order of the owner's keeps from the pet, a pet told to sit included.
 *
 * <p>The owner's order is never changed by it: a pet told to sit that gets up to run goes back to where it sat when it is
 * safe, and sits again, since the sitting is the directive's. The errand has no place of its own to go to, and the pet itself
 * is what the leash holds.
 */
public final class FleeErrand implements ErrandType<FleeErrand.Flight, ErrandTunings.Flee> {
    /**
     * What the pet has set out to do.
     *
     * @param seat where it was told to sit, if it was, which is where it goes back to
     */
    public record Flight(Optional<BlockPos> seat) implements Plan {
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.FLEE;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.SAFETY;
    }

    @Override
    public Codec<ErrandTunings.Flee> tuningCodec() {
        return ErrandTunings.Flee.CODEC;
    }

    @Override
    public Offers<Flight> offers(PetView view, ErrandTunings.Flee tuning) {
        if (view.attacker().isEmpty()) {
            return Offers.none(Check.fail("no_attacker"));
        }
        if (view.fightsBack()) {
            return Offers.none(Check.fail("fights_back"));
        }
        PetAnchor anchor = view.anchor();
        return Offers.of(Offer.of(new Flight(anchor.canMove() ? Optional.empty() : Optional.of(anchor.center().pos()))));
    }

    /** The pet hides until it is safe, even when what hurt it is long gone, and stops only if it can fight after all. */
    @Override
    public Check stillWanted(Flight plan, PetView view, ErrandTunings.Flee tuning) {
        return view.fightsBack() ? Check.fail("fights_back") : Check.OK;
    }

    @Override
    public Routine routine(Flight plan, ErrandSettings<ErrandTunings.Flee> settings) {
        ErrandTunings.Flee tuning = settings.tuning();
        List<Routine.Part> parts = new ArrayList<>();
        parts.add(Routine.step(() -> new RunTo(tuning)));
        parts.add(Routine.step(() -> new Cower(tuning)));
        plan.seat().ifPresent(seat -> parts.add(Routine.step(() -> WalkTo.near(new Goal.Place(seat), tuning.speed(), 0,
            tuning.arriveDistance(), settings.maxTicks()))));
        return Routine.of(parts.toArray(Routine.Part[]::new));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
