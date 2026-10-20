package com.dwinovo.chiikawa.platform.services;

import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPlacesSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetFarmerWorkSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetSocialSensor;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public interface IPlatformRegistryAccess {
    Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor();

    Supplier<SensorType<PetFarmerWorkSensor>> petFarmerWorkSensor();

    Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor();

    /** Finds the places a pet's day is built around: the nearest board and the nearest shop. */
    Supplier<SensorType<PetPlacesSensor>> petPlacesSensor();

    /** Notices the other pets a pet could play a scene with. */
    Supplier<SensorType<PetSocialSensor>> petSocialSensor();

    Supplier<Activity> socializeActivity();

    Supplier<Activity> cooperateActivity();

    Supplier<Activity> answerWhistleActivity();

    Supplier<MenuType<PetBackpackMenu>> petBackpackMenu();

    Supplier<LootItemConditionType> qualificationCondition();
}
