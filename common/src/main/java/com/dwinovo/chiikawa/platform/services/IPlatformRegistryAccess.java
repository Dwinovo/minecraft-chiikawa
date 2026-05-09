package com.dwinovo.chiikawa.platform.services;

import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetContainerSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetHarvestCropSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPlantCropSensor;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MenuType;

public interface IPlatformRegistryAccess {
    Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor();

    Supplier<SensorType<PetHarvestCropSensor>> petHarvestCropSensor();

    Supplier<SensorType<PetPlantCropSensor>> petPlantCropSensor();

    Supplier<SensorType<PetContainerSensor>> petContainerSensor();

    Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor();

    Supplier<Activity> farmerHarvestActivity();

    Supplier<Activity> farmerPlantActivity();

    Supplier<Activity> deleverActivity();

    /**
     * Brain {@link Activity} owning the fencer job's combat behaviors
     * (walk-to-target + melee swing). Replaces fencer's earlier reuse of
     * vanilla {@link Activity#WORK}, which conflicted with archer when both
     * jobs' behaviors were registered on the same brain.
     */
    Supplier<Activity> fencerFightActivity();

    /**
     * Brain {@link Activity} owning the archer job's ranged-attack behavior.
     * Sibling to {@link #fencerFightActivity} — distinct activity per job so
     * static brain registration doesn't conflate combat styles.
     */
    Supplier<Activity> archerShootActivity();

    Supplier<MenuType<PetBackpackMenu>> petBackpackMenu();
}
