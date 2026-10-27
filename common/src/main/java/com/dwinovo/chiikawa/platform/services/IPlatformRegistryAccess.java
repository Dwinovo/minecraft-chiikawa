package com.dwinovo.chiikawa.platform.services;

import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPlacesSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetWorkSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetSocialSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetSurroundingsSensor;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public interface IPlatformRegistryAccess {
    Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor();

    Supplier<SensorType<PetWorkSensor>> petWorkSensor();

    Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor();

    /** Finds the places a pet's day is built around: the nearest board and the nearest shop. */
    Supplier<SensorType<PetPlacesSensor>> petPlacesSensor();

    /** Notices the other pets a pet could play a scene with. */
    Supplier<SensorType<PetSocialSensor>> petSocialSensor();

    /** Looks at what is within a few blocks of a pet: shelter, creatures, things lying about and its owner coming back. */
    Supplier<SensorType<PetSurroundingsSensor>> petSurroundingsSensor();

    Supplier<MenuType<PetBackpackMenu>> petBackpackMenu();

    Supplier<LootItemConditionType> qualificationCondition();
}
