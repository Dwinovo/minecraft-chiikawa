package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Harvest;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Walks to the block the work sensor chose and breaks it into the bag: reaps a ripe crop, pulls up a weed, picks a
 * mushroom. The work's target says which, and its counter what each block counts towards a slip. A pet that could not
 * get its work done, because it could not find a way to the block or the block was not as it seemed on arrival,
 * leaves that block alone for a while and not other blocks.
 */
public final class HarvestBlockAction implements WorkAction<Site, ErrandTunings.HarvestBlock> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "harvest_block");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Codec<ErrandTunings.HarvestBlock> tuningCodec() {
        return ErrandTunings.HarvestBlock.CODEC;
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return WorkTarget.neededBy("harvest_block", target, WorkTarget.Kind.RIPE_CROP, WorkTarget.Kind.BLOCKS);
    }

    @Override
    public Offers<Site> offers(Work<Site, ErrandTunings.HarvestBlock> work, ResourceLocation errand, PetView view,
            ErrandTunings.HarvestBlock tuning) {
        return Site.offers(work, errand, view);
    }

    @Override
    public Check stillWanted(Work<Site, ErrandTunings.HarvestBlock> work, ResourceLocation errand, Site plan,
            PetView view, ErrandTunings.HarvestBlock tuning) {
        return plan.stillChosen(work, errand, view);
    }

    @Override
    public Routine routine(Work<Site, ErrandTunings.HarvestBlock> work, Site plan,
            ErrandSettings<ErrandTunings.HarvestBlock> settings) {
        ErrandTunings.HarvestBlock tuning = settings.tuning();
        BlockPos block = plan.where().pos();
        WorkTarget.BlockTarget kind = work.targetOf(WorkTarget.BlockTarget.class);
        return Routine.of(
            Site.walk(block, tuning.speed(), tuning.reachSqr(), settings.maxTicks(), tuning.noWayTicks()),
            Routine.step(() -> new Harvest(block, kind, work.counter(), tuning.toolWear())));
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.subjects();
    }
}
