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
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;

public final class FarmerJobHandler {
    private FarmerJobHandler() {
    }

    public static void initBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        BrainUtils.addCoreTasks(brain);
        BrainUtils.addIdleTasks(brain);
        // Harvest activity tasks.
        addFarmHarvestActivity(brain);
        // Plant activity tasks.
        addFarmPlantActivity(brain);
        // Deliver activity tasks.
        addDeliverActivity(brain,pet);

        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        
        
    }

    public static void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        updateActivity(brain);
    }

    private static void updateActivity(Brain<AbstractPet> brain) {
        Activity preferred = getPreferredActivity(brain);
        if (brain.isActive(preferred)) {
            return;
        }
        ImmutableList.Builder<Activity> activities = ImmutableList.builder();   
        // Priority: harvest > plant > deliver > idle.
        if (brain.hasMemoryValue(InitMemory.HARVEST_POS.get()))
        {
            activities.add(InitActivity.FARMER_HARVEST.get());
        }
        if (brain.hasMemoryValue(InitMemory.PLANT_POS.get()))
        {
            activities.add(InitActivity.FARMER_PLANT.get());
        }
        if (brain.hasMemoryValue(InitMemory.CONTAINER_POS.get()))
        {
            activities.add(InitActivity.DELEVER.get());
        }
        activities.add(Activity.IDLE);
        // Pick the highest valid activity.
        brain.setActiveActivityToFirstValid(activities.build());
    }

    private static Activity getPreferredActivity(Brain<AbstractPet> brain) {
        if (brain.hasMemoryValue(InitMemory.HARVEST_POS.get())) {
            return InitActivity.FARMER_HARVEST.get();
        }
        if (brain.hasMemoryValue(InitMemory.PLANT_POS.get())) {
            return InitActivity.FARMER_PLANT.get();
        }
        if (brain.hasMemoryValue(InitMemory.CONTAINER_POS.get())) {
            return InitActivity.DELEVER.get();
        }
        return Activity.IDLE;
    }
    
    private static void addFarmHarvestActivity(Brain<AbstractPet> brain) {
        BrainUtils.addActivity(brain, InitActivity.FARMER_HARVEST.get(), ImmutableList.of(
            Pair.of(3, new HarvestCropBehavior()),
            Pair.of(4, new WalkToHarvestCropBehavior(0.8F))
        ));
    }
    private static void addFarmPlantActivity(Brain<AbstractPet> brain) {
        BrainUtils.addActivity(brain, InitActivity.FARMER_PLANT.get(), ImmutableList.of(
            Pair.of(3, new PlantCropBehavior()),
            Pair.of(4, new WalkToPlantCropBehavior(0.8F))
        ));
    }
    private static void addDeliverActivity(Brain<AbstractPet> brain,AbstractPet pet) {
        BrainUtils.addActivity(brain, InitActivity.DELEVER.get(), ImmutableList.of(
            Pair.of(3, new DeliverCropBehavior()),
            Pair.of(4, new WalkToContainerBehavior(0.8F))
        ));
    }
}
