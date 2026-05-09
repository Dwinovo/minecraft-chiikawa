package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetContainerSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetHarvestCropSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPlantCropSensor;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.platform.services.IPlatformRegistryAccess;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgePlatformRegistryAccess implements IPlatformRegistryAccess {
    private static final DeferredRegister<SensorType<?>> SENSOR_TYPES =
        DeferredRegister.create(Registries.SENSOR_TYPE, Constants.MOD_ID);
    private static final DeferredRegister<Activity> ACTIVITIES =
        DeferredRegister.create(Registries.ACTIVITY, Constants.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    private static final DeferredHolder<SensorType<?>, SensorType<PetAttackbleEntitySensor>> PET_ATTACKBLE_ENTITY_SENSOR =
        SENSOR_TYPES.register("pet_attackble_entity_sensor", () -> new SensorType<>(PetAttackbleEntitySensor::new));
    private static final DeferredHolder<SensorType<?>, SensorType<PetHarvestCropSensor>> PET_HARVEST_CROP_SENSOR =
        SENSOR_TYPES.register("pet_harvest_crop_sensor", () -> new SensorType<>(PetHarvestCropSensor::new));
    private static final DeferredHolder<SensorType<?>, SensorType<PetPlantCropSensor>> PET_PLANT_CROP_SENSOR =
        SENSOR_TYPES.register("pet_plant_crop_sensor", () -> new SensorType<>(PetPlantCropSensor::new));
    private static final DeferredHolder<SensorType<?>, SensorType<PetContainerSensor>> PET_CONTAINER_SENSOR =
        SENSOR_TYPES.register("pet_container_sensor", () -> new SensorType<>(PetContainerSensor::new));
    private static final DeferredHolder<SensorType<?>, SensorType<PetPickableItemSensor>> PET_ITEM_ENTITY_SENSOR =
        SENSOR_TYPES.register("pet_item_entity_sensor", () -> new SensorType<>(PetPickableItemSensor::new));

    private static final DeferredHolder<Activity, Activity> FARMER_HARVEST =
        ACTIVITIES.register("farmer_harvest", () -> new Activity("farmer_harvest"));
    private static final DeferredHolder<Activity, Activity> FARMER_PLANT =
        ACTIVITIES.register("farmer_plant", () -> new Activity("farmer_plant"));
    private static final DeferredHolder<Activity, Activity> DELEVER =
        ACTIVITIES.register("delever", () -> new Activity("delever"));
    private static final DeferredHolder<Activity, Activity> FENCER_FIGHT =
        ACTIVITIES.register("fencer_fight", () -> new Activity("fencer_fight"));
    private static final DeferredHolder<Activity, Activity> ARCHER_SHOOT =
        ACTIVITIES.register("archer_shoot", () -> new Activity("archer_shoot"));

    private static final DeferredHolder<MenuType<?>, MenuType<PetBackpackMenu>> PET_BACKPACK =
        MENUS.register("pet_backpack", () -> IMenuTypeExtension.create((containerId, inventory, buf) ->
            new PetBackpackMenu(containerId, inventory)
        ));

    public static void register(IEventBus modEventBus) {
        SENSOR_TYPES.register(modEventBus);
        ACTIVITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }

    @Override
    public Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor() {
        return PET_ATTACKBLE_ENTITY_SENSOR;
    }

    @Override
    public Supplier<SensorType<PetHarvestCropSensor>> petHarvestCropSensor() {
        return PET_HARVEST_CROP_SENSOR;
    }

    @Override
    public Supplier<SensorType<PetPlantCropSensor>> petPlantCropSensor() {
        return PET_PLANT_CROP_SENSOR;
    }

    @Override
    public Supplier<SensorType<PetContainerSensor>> petContainerSensor() {
        return PET_CONTAINER_SENSOR;
    }

    @Override
    public Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor() {
        return PET_ITEM_ENTITY_SENSOR;
    }

    @Override
    public Supplier<Activity> farmerHarvestActivity() {
        return FARMER_HARVEST;
    }

    @Override
    public Supplier<Activity> farmerPlantActivity() {
        return FARMER_PLANT;
    }

    @Override
    public Supplier<Activity> deleverActivity() {
        return DELEVER;
    }

    @Override
    public Supplier<Activity> fencerFightActivity() {
        return FENCER_FIGHT;
    }

    @Override
    public Supplier<Activity> archerShootActivity() {
        return ARCHER_SHOOT;
    }

    @Override
    public Supplier<MenuType<PetBackpackMenu>> petBackpackMenu() {
        return PET_BACKPACK;
    }
}
