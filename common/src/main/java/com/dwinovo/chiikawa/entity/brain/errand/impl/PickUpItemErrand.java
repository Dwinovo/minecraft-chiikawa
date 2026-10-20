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
import com.dwinovo.chiikawa.entity.brain.errand.step.PickUp;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.view.PickableItem;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Walks to an item lying about that the pets pick up and takes it. The item the sensor remembers when the
 * pet decides is the one it goes for, and it keeps to that one: another that turns up nearer on the way is
 * for the next errand.
 *
 * <p>The item is the plan's subject, so an item the pet could not get to or did not fit is left alone for
 * a while and not tried for again tick after tick, while another item is not held back by it.
 */
public final class PickUpItemErrand implements ErrandType<PickUpItemErrand.Fetch, ErrandTunings.PickUp> {
    /** The brain's own mover takes the pet right up to the item; the walk is done when the pet is near enough to pick it up. */
    private static final int RIGHT_UP_TO_IT = 0;

    /**
     * What the pet has set out to fetch.
     *
     * @param item the item entity's id
     */
    public record Fetch(UUID item) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return view.pickable(item).map(PickableItem::where);
        }

        @Override
        public Optional<String> subject() {
            return Optional.of(item.toString());
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.PICK_UP_ITEM;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.PICK_UP;
    }

    @Override
    public Codec<ErrandTunings.PickUp> tuningCodec() {
        return ErrandTunings.PickUp.CODEC;
    }

    @Override
    public Offers<Fetch> offers(PetView view, ErrandTunings.PickUp tuning) {
        return view.targets().pickableItem()
            .map(item -> Offers.of(Offer.of(new Fetch(item.id()))))
            .orElseGet(() -> Offers.none(Check.fail("no_item")));
    }

    @Override
    public Check stillWanted(Fetch plan, PetView view, ErrandTunings.PickUp tuning) {
        return view.pickable(plan.item()).isPresent() ? Check.OK : Check.fail("no_item");
    }

    @Override
    public Routine routine(Fetch plan, ErrandSettings<ErrandTunings.PickUp> settings) {
        ErrandTunings.PickUp tuning = settings.tuning();
        return Routine.of(
            Routine.step(() -> new WalkTo(new Goal.Creature(plan.item()), tuning.speed(), RIGHT_UP_TO_IT,
                tuning.pickupDistance(), tuning.maxWalkTicks(), tuning.noWayTicks())),
            Routine.step(() -> new PickUp(plan.item())));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.subjects();
    }
}
