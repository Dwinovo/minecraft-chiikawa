package com.dwinovo.chiikawa.entity.brain.handler;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.task.farmer.DeliverCropBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.HarvestCropBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.PlantCropBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.WalkToContainerBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.WalkToHarvestCropBehavior;
import com.dwinovo.chiikawa.entity.brain.task.farmer.WalkToPlantCropBehavior;
import com.dwinovo.chiikawa.init.InitActivity;
import com.dwinovo.chiikawa.init.InitMemory;
import com.dwinovo.chiikawa.utils.BrainUtils;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Brain wiring for the {@code FARMER} pet job. Registered in
 * {@link com.dwinovo.chiikawa.entity.AbstractPet#makeBrain} alongside every
 * other job's activities — the brain is built once at entity construction
 * and never rebuilt. {@link #tickBrain} runs every server tick (only when
 * {@code FARMER} is the active job) and chooses which farmer activity to
 * activate based on which work-target memory is currently set.
 */
public final class FarmerJobHandler {
    private FarmerJobHandler() {
    }

    /**
     * Register the farmer job's activity → behavior bindings on the supplied
     * brain. Called once per pet at brain construction (regardless of which
     * job is currently active); behaviors only fire while their owning
     * activity is selected by {@link #tickBrain}.
     */
    public static void registerActivities(Brain<AbstractPet> brain) {
        BrainUtils.addActivity(brain, InitActivity.FARMER_HARVEST.get(), ImmutableList.of(
            Pair.of(3, new HarvestCropBehavior()),
            Pair.of(4, new WalkToHarvestCropBehavior(0.8F))
        ));
        BrainUtils.addActivity(brain, InitActivity.FARMER_PLANT.get(), ImmutableList.of(
            Pair.of(3, new PlantCropBehavior()),
            Pair.of(4, new WalkToPlantCropBehavior(0.8F))
        ));
        BrainUtils.addActivity(brain, InitActivity.DELEVER.get(), ImmutableList.of(
            Pair.of(3, new DeliverCropBehavior()),
            Pair.of(4, new WalkToContainerBehavior(0.8F))
        ));
    }

    public static void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        ImmutableList.Builder<Activity> activities = ImmutableList.builder();
        // Priority: harvest > plant > deliver > idle.
        if (brain.hasMemoryValue(InitMemory.HARVEST_POS.get())) {
            activities.add(InitActivity.FARMER_HARVEST.get());
        }
        if (brain.hasMemoryValue(InitMemory.PLANT_POS.get())) {
            activities.add(InitActivity.FARMER_PLANT.get());
        }
        if (brain.hasMemoryValue(InitMemory.CONTAINER_POS.get())) {
            activities.add(InitActivity.DELEVER.get());
        }
        activities.add(Activity.IDLE);
        brain.setActiveActivityToFirstValid(activities.build());
    }
}
