package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.entity.brain.task.farmer.DeliverCropBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.HarvestCropBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.PlantCropBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.WalkToContainerBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.WalkToHarvestCropBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.WalkToPlantCropBehavior;
import com.dwinovo.chiikawa.init.InitActivity;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.world.entity.ai.Brain;

/**
 * Brain wiring for the {@code FARMER} pet job. Registered in
 * {@link com.dwinovo.chiikawa.entity.AbstractPet#makeBrain} alongside every
 * other job's activities — the brain is built once at entity construction
 * and never rebuilt. The {@code harvest}, {@code plant} and {@code deliver}
 * intents decide which farmer activity runs, based on which work-target
 * memory the farmer sensor has set.
 *
 * <p>Leaving an activity keeps its work-target memory: the farmer sensor owns
 * those and re-validates them, and a plant target interrupted by a harvest is
 * picked up again right after.
 */
public final class FarmerJobHandler {
    private FarmerJobHandler() {
    }

    /**
     * Register the farmer job's activity → behavior bindings on the supplied
     * brain. Called once per pet at brain construction (regardless of which
     * job is currently active); behaviors only fire while their owning
     * activity is selected.
     */
    public static void registerActivities(Brain<AbstractPet> brain) {
        PetActivities.register(brain, InitActivity.FARMER_HARVEST.get(), ImmutableList.of(
            Pair.of(3, new HarvestCropBehavior()),
            Pair.of(4, new WalkToHarvestCropBehavior(0.8F))
        ), Set.of());
        PetActivities.register(brain, InitActivity.FARMER_PLANT.get(), ImmutableList.of(
            Pair.of(3, new PlantCropBehavior()),
            Pair.of(4, new WalkToPlantCropBehavior(0.8F))
        ), Set.of());
        PetActivities.register(brain, InitActivity.DELEVER.get(), ImmutableList.of(
            Pair.of(3, new DeliverCropBehavior()),
            Pair.of(4, new WalkToContainerBehavior(0.8F))
        ), Set.of());
    }
}
