package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetFarmerWorkSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.sensing.SensorType;

public final class InitSensor {
    public static final Supplier<SensorType<PetAttackbleEntitySensor>> PET_ATTACKBLE_ENTITY_SENSOR =
        Services.PLATFORM_REGISTRY.petAttackbleEntitySensor();
    public static final Supplier<SensorType<PetFarmerWorkSensor>> PET_FARMER_WORK_SENSOR =
        Services.PLATFORM_REGISTRY.petFarmerWorkSensor();
    public static final Supplier<SensorType<PetPickableItemSensor>> PET_ITEM_ENTITY_SENSOR =
        Services.PLATFORM_REGISTRY.petItemEntitySensor();

    private InitSensor() {
    }

    public static void init() {
    }
}
