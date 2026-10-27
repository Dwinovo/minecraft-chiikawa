package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPlacesSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetSocialSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetSurroundingsSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetWorkSensor;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.sensing.SensorType;

public final class InitSensor {
    public static final Supplier<SensorType<PetAttackbleEntitySensor>> PET_ATTACKBLE_ENTITY_SENSOR =
        Services.PLATFORM_REGISTRY.petAttackbleEntitySensor();
    public static final Supplier<SensorType<PetWorkSensor>> PET_WORK_SENSOR =
        Services.PLATFORM_REGISTRY.petWorkSensor();
    public static final Supplier<SensorType<PetPickableItemSensor>> PET_ITEM_ENTITY_SENSOR =
        Services.PLATFORM_REGISTRY.petItemEntitySensor();
    public static final Supplier<SensorType<PetPlacesSensor>> PET_PLACES_SENSOR =
        Services.PLATFORM_REGISTRY.petPlacesSensor();
    public static final Supplier<SensorType<PetSocialSensor>> PET_SOCIAL_SENSOR =
        Services.PLATFORM_REGISTRY.petSocialSensor();

    public static final Supplier<SensorType<PetSurroundingsSensor>> PET_SURROUNDINGS_SENSOR =
        Services.PLATFORM_REGISTRY.petSurroundingsSensor();

    private InitSensor() {
    }

    public static void init() {
    }
}
