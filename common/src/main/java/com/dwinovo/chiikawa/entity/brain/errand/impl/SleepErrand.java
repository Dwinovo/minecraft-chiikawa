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
import com.dwinovo.chiikawa.entity.brain.errand.step.Sleep;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Lies down to sleep: a pet that is free, or wild, at night, near home, and a pet at heel while its owner sleeps. A bed that
 * nobody is in and no pet is on, if there is one within the pet's reach, is slept on, as a cat sleeps on one; and where
 * there is none the pet lies down on the ground, by its owner if it is at heel and at home if it is not. The pet only lies there, as a cat does in
 * a bed, and never sleeps in it as a player does: the bed is not taken from its owner, and the owner can get into it at any
 * time.
 *
 * <p>It is something to do with idle time, and so is no match for anything that matters more: work at night, such as picking
 * mushrooms, a fight and getting out of harm's way all wake the pet. The pet wakes by itself with the morning, or, if it
 * sleeps because its owner does, when the owner gets up.
 */
public final class SleepErrand implements ErrandType<SleepErrand.Resting, ErrandTunings.Sleep> {
    /** What the pet has set out to do: where it lies down, and what wakes it. */
    public sealed interface Resting extends Plan {
        /** @return what wakes it */
        Sleep.Wake wake();
    }

    /**
     * Lie down on a bed.
     *
     * @param bed the bed
     * @param wake what wakes the pet
     */
    public record OnBed(GlobalPos bed, Sleep.Wake wake) implements Resting {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return Optional.of(bed);
        }
    }

    /**
     * Lie down on the ground.
     *
     * @param spot where
     * @param wake what wakes the pet
     */
    public record OnGround(GlobalPos spot, Sleep.Wake wake) implements Resting {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return Optional.of(spot);
        }
    }

    /**
     * @return how each character sleeps, as the settings of this errand have it: the one account of how a pet looks asleep, for
     *         whatever else has a pet lie quiet, such as waiting by a bed; nothing if the errand has no settings
     */
    public static Optional<ErrandTunings.Performances> sleeps() {
        return ErrandSettings.of(ErrandIds.SLEEP).map(ErrandSettings::tuning).filter(ErrandTunings.Sleep.class::isInstance)
            .map(tuning -> ((ErrandTunings.Sleep) tuning).sleeps());
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.SLEEP;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.SLEEP;
    }

    @Override
    public Codec<ErrandTunings.Sleep> tuningCodec() {
        return ErrandTunings.Sleep.CODEC;
    }

    @Override
    public Offers<Resting> offers(PetView view, ErrandTunings.Sleep tuning) {
        Sleep.Wake wake;
        if (view.anchor().followsOwner()) {
            if (view.ownerBedside().isEmpty()) {
                return Offers.none(Check.fail("owner_awake"));
            }
            wake = Sleep.Wake.WITH_OWNER;
        } else {
            // A level that is always dark, as the Nether is, has no night to sleep through.
            if (view.phase() != DayPhase.NIGHT || !view.bedsWork()) {
                return Offers.none(Check.fail("not_night"));
            }
            wake = Sleep.Wake.AT_MORNING;
        }
        Optional<GlobalPos> bed = view.freeBed().filter(found -> view.anchor().admits(found, true));
        Resting plan = bed.<Resting>map(found -> new OnBed(found, wake))
            .orElseGet(() -> new OnGround(groundFor(view), wake));
        return Offers.of(Offer.of(plan));
    }

    /**
     * @return where it lies down with no bed to lie on: where it stands, if it is at heel, which is by its owner and not in
     *         their bed, and at home if it is not
     */
    private static GlobalPos groundFor(PetView view) {
        return view.anchor().followsOwner() ? view.pos() : view.anchor().center();
    }

    @Override
    public Routine routine(Resting plan, ErrandSettings<ErrandTunings.Sleep> settings) {
        ErrandTunings.Sleep tuning = settings.tuning();
        GlobalPos where = switch (plan) {
            case OnBed onBed -> onBed.bed();
            case OnGround onGround -> onGround.spot();
        };
        return Routine.of(
            Routine.step(() -> WalkTo.near(new Goal.Place(where.pos()), tuning.speed(), tuning.arriveDistance(), tuning.reach(),
                settings.maxTicks())),
            Routine.step(() -> new Sleep(plan instanceof OnBed ? Optional.of(where.pos()) : Optional.empty(), plan.wake(),
                tuning.sleeps())));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.shared();
    }
}
