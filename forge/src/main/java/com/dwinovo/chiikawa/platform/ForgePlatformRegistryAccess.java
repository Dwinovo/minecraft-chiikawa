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
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryObject;

public final class ForgePlatformRegistryAccess implements IPlatformRegistryAccess {
    // Sensor Types
    private static final DeferredRegister<SensorType<?>> SENSOR_TYPES =
        DeferredRegister.create(ForgeRegistries.SENSOR_TYPES, Constants.MOD_ID);
    // Activities
    private static final DeferredRegister<Activity> ACTIVITIES =
        DeferredRegister.create(ForgeRegistries.ACTIVITIES, Constants.MOD_ID);
    // Menus
    private static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, Constants.MOD_ID);

    // Sensor registrations
    private static final RegistryObject<SensorType<PetAttackbleEntitySensor>> PET_ATTACKBLE_ENTITY_SENSOR =
        SENSOR_TYPES.register("pet_attackble_entity_sensor", () -> new SensorType<>(PetAttackbleEntitySensor::new));
    private static final RegistryObject<SensorType<PetHarvestCropSensor>> PET_HARVEST_CROP_SENSOR =
        SENSOR_TYPES.register("pet_harvest_crop_sensor", () -> new SensorType<>(PetHarvestCropSensor::new));
    private static final RegistryObject<SensorType<PetPlantCropSensor>> PET_PLANT_CROP_SENSOR =
        SENSOR_TYPES.register("pet_plant_crop_sensor", () -> new SensorType<>(PetPlantCropSensor::new));
    private static final RegistryObject<SensorType<PetContainerSensor>> PET_CONTAINER_SENSOR =
        SENSOR_TYPES.register("pet_container_sensor", () -> new SensorType<>(PetContainerSensor::new));
    private static final RegistryObject<SensorType<PetPickableItemSensor>> PET_ITEM_ENTITY_SENSOR =
        SENSOR_TYPES.register("pet_item_entity_sensor", () -> new SensorType<>(PetPickableItemSensor::new));

    // Activity registrations
    private static final RegistryObject<Activity> FARMER_HARVEST =
        ACTIVITIES.register("farmer_harvest", () -> new Activity("farmer_harvest"));
    private static final RegistryObject<Activity> FARMER_PLANT =
        ACTIVITIES.register("farmer_plant", () -> new Activity("farmer_plant"));
    private static final RegistryObject<Activity> DELEVER =
        ACTIVITIES.register("delever", () -> new Activity("delever"));

    // Menu registrations
    private static final RegistryObject<MenuType<PetBackpackMenu>> PET_BACKPACK =
        MENUS.register("pet_backpack", () -> IForgeMenuType.create((containerId, inventory, buf) ->
            new PetBackpackMenu(containerId, inventory)
        ));

    public static void register(IEventBus modEventBus) {
        // Register NewRegistryEvent listener to capture custom registry
        modEventBus.addListener(ForgePlatformRegistryAccess::onNewRegistry);
        
        // Register all DeferredRegisters
        SENSOR_TYPES.register(modEventBus);
        ACTIVITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }

    private static void onNewRegistry(NewRegistryEvent event) {
        // No-op for now, as custom registries are handled by ForgeRegistryHelper
    }

    // IPlatformRegistryAccess implementations
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
    public Supplier<MenuType<PetBackpackMenu>> petBackpackMenu() {
        return PET_BACKPACK;
    }
}
