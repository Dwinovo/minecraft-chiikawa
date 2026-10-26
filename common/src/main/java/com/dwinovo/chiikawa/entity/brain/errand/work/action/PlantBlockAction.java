package com.dwinovo.chiikawa.entity.brain.errand.work.action;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.Check;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandSettings;
import com.dwinovo.chiikawa.entity.brain.errand.ErrandTunings;
import com.dwinovo.chiikawa.entity.brain.errand.Offers;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestStore;
import com.dwinovo.chiikawa.entity.brain.errand.run.Routine;
import com.dwinovo.chiikawa.entity.brain.errand.step.Plant;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.work.Work;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkAction;
import com.dwinovo.chiikawa.entity.brain.errand.work.WorkTarget;
import com.dwinovo.chiikawa.utils.Utils;
import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Walks to the bare soil the work sensor chose and plants a seed from the bag in it. Which seed is the soil's to say,
 * see {@link Plant}. A pet that could not get its work done, because it could not find a way to the soil or the soil
 * was not as it seemed on arrival, leaves that soil alone for a while and not other soil.
 */
public final class PlantBlockAction implements WorkAction<Site, ErrandTunings.PlantBlock> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "plant_block");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Codec<ErrandTunings.PlantBlock> tuningCodec() {
        return ErrandTunings.PlantBlock.CODEC;
    }

    @Override
    public Optional<String> refuses(Optional<WorkTarget> target) {
        return WorkTarget.neededBy("plant_block", target, WorkTarget.Kind.PLANTABLE_SOIL);
    }

    @Override
    public Offers<Site> offers(Work<Site, ErrandTunings.PlantBlock> work, ResourceLocation errand, PetView view,
            ErrandTunings.PlantBlock tuning) {
        return Site.offers(work, errand, view);
    }

    @Override
    public Check stillWanted(Work<Site, ErrandTunings.PlantBlock> work, ResourceLocation errand, Site plan,
            PetView view, ErrandTunings.PlantBlock tuning) {
        return plan.stillChosen(work, errand, view);
    }

    @Override
    public Routine routine(Work<Site, ErrandTunings.PlantBlock> work, Site plan,
            ErrandSettings<ErrandTunings.PlantBlock> settings) {
        ErrandTunings.PlantBlock tuning = settings.tuning();
        BlockPos soil = plan.where().pos();
        return Routine.of(
            Site.walk(soil, tuning.speed(), tuning.reachSqr(), settings.maxTicks(), tuning.noWayTicks()),
            Routine.step(() -> new Plant(soil)));
    }

    /** The seeds, as the crop handlers say which they are. */
    @Override
    public Predicate<ItemStack> materials() {
        return Utils::isSeed;
    }

    @Override
    public RestStore.Kind restStore() {
        return RestStore.subjects();
    }
}
