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
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSources;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.HandOver;
import com.dwinovo.chiikawa.entity.brain.errand.step.PlaceByBed;
import com.dwinovo.chiikawa.entity.brain.errand.step.TakePresent;
import com.dwinovo.chiikawa.entity.brain.errand.step.WaitForWaking;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.gifting.GiftingSettings;
import com.mojang.serialization.Codec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Takes the owner the present the pet is carrying, which {@link OfferGiftErrand} took out of the bag. It starts only
 * while the pet carries one, a pet that is carrying a present from before it was saved included, and goes on only
 * while it does.
 *
 * <p>Its priority is above everything a pet does for itself. A gift is the one errand where the
 * pet wants to be seen doing it, and a cake handed over three chores later is not the
 * same cake: an owner has to be able to connect the present to the pet standing in front
 * of them holding it. A fight at the same priority comes first.
 *
 * <p>An owner who is awake is walked to and handed the present, into their hands where there is room and at their feet
 * where there is not. An owner asleep in a bed is given it as a cat gives its owner a morning gift: the pet waits by the
 * bed, lying quiet, and leaves it there when the owner wakes in the morning. It is given by the owner taking it, too:
 * the errand ends as the paws are empty. If the errand is dropped on the way, because the owner has gone or the pet was
 * told to sit, the present goes back in the bag.
 *
 * <p>The giving rests the pet for as long as the gifting settings say, see {@code Presents}; an errand that gives up rests
 * half a minute, so that a present put back is not taken out again at once, and one that is cut short rests not at all,
 * since nothing was wrong with it.
 */
public final class GiftOwnerErrand implements ErrandType<GiftOwnerErrand.Presenting, ErrandTunings.GiftOwner> {
    /** What it set out to do: take the present to the owner. */
    public sealed interface Presenting extends Plan permits Delivery, Bedside {
    }

    /** Take the present to the owner, wherever they are now. */
    public record Delivery() implements Presenting {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.ownerPos();
        }
    }

    /**
     * Leave the present by the owner's bed.
     *
     * @param where the place by the bed the pet waits at
     */
    public record Bedside(GlobalPos where) implements Presenting {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return Optional.of(where);
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.GIFT_OWNER;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.GIFT;
    }

    @Override
    public Codec<ErrandTunings.GiftOwner> tuningCodec() {
        return ErrandTunings.GiftOwner.CODEC;
    }

    @Override
    public Offers<Presenting> offers(PetView view, ErrandTunings.GiftOwner tuning) {
        if (!view.carryingGift()) {
            return Offers.none(Check.fail("no_gift"));
        }
        Presenting plan = view.ownerBedside().<Presenting>map(Bedside::new).orElseGet(Delivery::new);
        return Offers.of(Offer.of(plan));
    }

    @Override
    public Check stillWanted(Presenting plan, PetView view, ErrandTunings.GiftOwner tuning) {
        return view.carryingGift() ? Check.OK : Check.fail("no_gift");
    }

    /**
     * The walk has no limit of its own and none for a pet that cannot find a way: a long way round is long enough
     * to cross a garden, and the errand's time limit is what ends it.
     */
    @Override
    public Routine routine(Presenting plan, ErrandSettings<ErrandTunings.GiftOwner> settings) {
        ErrandTunings.GiftOwner tuning = settings.tuning();
        return switch (plan) {
            case Delivery delivery -> Routine.of(
                Routine.step(() -> new WalkTo(new Goal.Owner(), tuning.speed(), tuning.arriveDistance(), tuning.reach(),
                    settings.maxTicks(), settings.maxTicks())),
                Routine.step(HandOver::new)).releasing(TakePresent::putBack);
            case Bedside bedside -> Routine.of(
                Routine.step(() -> WalkTo.near(new Goal.Place(bedside.where().pos()), tuning.speed(), tuning.arriveDistance(),
                    tuning.reach(), settings.maxTicks())),
                Routine.step(() -> new WaitForWaking(SleepErrand.sleeps())),
                Routine.step(() -> new PlaceByBed(tuning.morningFrom(), tuning.morningTo(), tuning.sleptTicks())))
                .releasing(TakePresent::putBack);
        };
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.giftCooldown();
    }

    @Override
    public Map<ResourceLocation, RestSources.Resolver> restSources() {
        return Map.of(RestSources.GIFTING_COOLDOWN,
            context -> GiftingSettings.current().map(GiftingSettings::cooldownTicks).orElse(0));
    }
}
