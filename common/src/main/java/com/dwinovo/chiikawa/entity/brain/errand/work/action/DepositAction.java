package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.CloseContainer;
import com.dwinovo.chiikawa.entity.brain.errand.step.DepositOne;
import com.dwinovo.chiikawa.entity.brain.errand.step.Goal;
import com.dwinovo.chiikawa.entity.brain.errand.step.Lid;
import com.dwinovo.chiikawa.entity.brain.errand.step.OpenContainer;
import com.dwinovo.chiikawa.entity.brain.errand.step.WalkTo;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.dwinovo.chiikawa.utils.Deliveries;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Takes what the pet has to deliver to the container the work sensor chose and puts it in, one thing at a time for as
 * long as there is something to put in and room for it. It starts only on a container within the anchor's reach, keeps
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
public final class DepositAction implements WorkAction<Site, ErrandTunings.Deliver> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "deposit");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Codec<ErrandTunings.Deliver> tuningCodec() {
        return ErrandTunings.Deliver.CODEC;
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return WorkTarget.neededBy("deposit", target, WorkTarget.Kind.CONTAINER);
    }

    @Override
    public Offers<Site> offers(Work<Site, ErrandTunings.Deliver> work, ResourceLocation errand, PetView view,
            ErrandTunings.Deliver tuning) {
        return Site.offers(work, errand, view);
    }

    @Override
    public Routine routine(Work<Site, ErrandTunings.Deliver> work, Site plan,
            ErrandSettings<ErrandTunings.Deliver> settings) {
        ErrandTunings.Deliver tuning = settings.tuning();
        BlockPos container = plan.where().pos();
        Deliveries deliveries = work.targetOf(WorkTarget.Container.class).deliveries();
        Lid lid = new Lid(container);
        return Routine.of(
            Routine.step(() -> new WalkTo(new Goal.Container(container, deliveries), tuning.speed(), tuning.arriveDistance(),
                tuning.reach(), settings.maxTicks(), tuning.noWayTicks())),
            Routine.step(() -> new OpenContainer(container, lid, deliveries)),
            Routine.repeatWhile(DepositOne.whileItemsFit(container, deliveries),
                () -> new DepositOne(container, deliveries)),
            Routine.step(() -> new CloseContainer(lid))).releasing(lid::lowerIfRaised);
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.subjects();
    }
}
