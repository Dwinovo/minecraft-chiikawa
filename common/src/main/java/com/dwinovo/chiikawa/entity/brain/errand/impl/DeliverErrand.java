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
import com.dwinovo.chiikawa.entity.brain.errand.rest.SubjectRests;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.CloseContainer;
import com.dwinovo.chiikawa.entity.brain.errand.step.DepositOne;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.Lid;
import com.dwinovo.chiikawa.entity.brain.errand.step.OpenContainer;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Takes what the pet has to deliver to the container the farmer sensor remembers and puts it in, one thing at a time for
 * as long as there is something to put in and room for it. It starts only on a container within the anchor's reach, keeps
 * going while it stays within the leash, and keeps to the container it set off for.
 *
 * <p>Whether the container is still worth the walk is the walk's to say, see {@link Goal.Container}, and when the last
 * thing is in is the routine's: the pet is not asked if it still wants to go on at a moment when its bag has just been
 * emptied, which is the delivery done and not given up.
 *
 * <p>A chest is opened before the pet fills it and closed when it has done. The lid is held by the routine, so a pet that
 * is taken off the errand with the chest open has it closed for it, whatever took it off. A pet that could not find a way
 * to the container, or found nothing to put in it, leaves that container alone for a while.
 */
public final class DeliverErrand implements ErrandType<DeliverErrand.Trip, ErrandTunings.Deliver> {
    /**
     * What the pet has set out to do.
     *
     * @param container the container's block
     */
    public record Trip(GlobalPos container) implements Plan {
        @Override
        public Optional<GlobalPos> destination(PetView view) {
            return Optional.of(container);
        }

        @Override
        public Optional<String> subject() {
            return Optional.of(SubjectRests.subject(container.pos()));
        }
    }

    @Override
    public ResourceLocation id() {
        return ErrandIds.DELIVER;
    }

    @Override
    public ErrandCategory category() {
        return ErrandCategory.WORK;
    }

    @Override
    public Codec<ErrandTunings.Deliver> tuningCodec() {
        return ErrandTunings.Deliver.CODEC;
    }

    @Override
    public Offers<Trip> offers(PetView view, ErrandTunings.Deliver tuning) {
        return view.targets().container()
            .map(container -> Offers.of(Offer.of(new Trip(container))))
            .orElseGet(() -> Offers.none(Check.fail("no_container")));
    }

    @Override
    public Routine routine(Trip plan, ErrandSettings<ErrandTunings.Deliver> settings) {
        ErrandTunings.Deliver tuning = settings.tuning();
        BlockPos container = plan.container().pos();
        Lid lid = new Lid(container);
        return Routine.of(
            Routine.step(() -> new WalkTo(new Goal.Container(container), tuning.speed(), tuning.arriveDistance(), tuning.reach(),
                settings.maxTicks(), tuning.noWayTicks())),
            Routine.step(() -> new OpenContainer(container, lid)),
            Routine.repeatWhile(DepositOne.whileItemsFit(container), () -> new DepositOne(container)),
            Routine.step(() -> new CloseContainer(lid))).releasing(lid::lowerIfRaised);
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.subjects();
    }
}
