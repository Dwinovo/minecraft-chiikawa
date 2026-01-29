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
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MenuType;

public final class FabricPlatformRegistryAccess implements IPlatformRegistryAccess {
    private final Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor;
    private final Supplier<SensorType<PetHarvestCropSensor>> petHarvestCropSensor;
    private final Supplier<SensorType<PetPlantCropSensor>> petPlantCropSensor;
    private final Supplier<SensorType<PetContainerSensor>> petContainerSensor;
    private final Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor;
    private final Supplier<Activity> farmerHarvestActivity;
    private final Supplier<Activity> farmerPlantActivity;
    private final Supplier<Activity> deleverActivity;
    private final Supplier<MenuType<PetBackpackMenu>> petBackpackMenu;

    public FabricPlatformRegistryAccess() {
        petAttackbleEntitySensor = registerSensor("pet_attackble_entity_sensor", new SensorType<>(PetAttackbleEntitySensor::new));
        petHarvestCropSensor = registerSensor("pet_harvest_crop_sensor", new SensorType<>(PetHarvestCropSensor::new));
        petPlantCropSensor = registerSensor("pet_plant_crop_sensor", new SensorType<>(PetPlantCropSensor::new));
        petContainerSensor = registerSensor("pet_container_sensor", new SensorType<>(PetContainerSensor::new));
        petItemEntitySensor = registerSensor("pet_item_entity_sensor", new SensorType<>(PetPickableItemSensor::new));

        farmerHarvestActivity = registerActivity("farmer_harvest", new Activity("farmer_harvest"));
        farmerPlantActivity = registerActivity("farmer_plant", new Activity("farmer_plant"));
        deleverActivity = registerActivity("delever", new Activity("delever"));

        petBackpackMenu = registerMenu("pet_backpack", new MenuType<>(PetBackpackMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    }

    private static <T extends SensorType<?>> Supplier<T> registerSensor(String path, T type) {
        Registry.register(BuiltInRegistries.SENSOR_TYPE, id(path), type);
        return () -> type;
    }

    private static Supplier<Activity> registerActivity(String path, Activity activity) {
        Registry.register(BuiltInRegistries.ACTIVITY, id(path), activity);
        return () -> activity;
    }

    private static Supplier<MenuType<PetBackpackMenu>> registerMenu(String path, MenuType<PetBackpackMenu> menuType) {
        Registry.register(BuiltInRegistries.MENU, id(path), menuType);
        return () -> menuType;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Constants.MOD_ID, path);
    }

    @Override
    public Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor() {
        return petAttackbleEntitySensor;
    }

    @Override
    public Supplier<SensorType<PetHarvestCropSensor>> petHarvestCropSensor() {
        return petHarvestCropSensor;
    }

    @Override
    public Supplier<SensorType<PetPlantCropSensor>> petPlantCropSensor() {
        return petPlantCropSensor;
    }

    @Override
    public Supplier<SensorType<PetContainerSensor>> petContainerSensor() {
        return petContainerSensor;
    }

    @Override
    public Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor() {
        return petItemEntitySensor;
    }

    @Override
    public Supplier<Activity> farmerHarvestActivity() {
        return farmerHarvestActivity;
    }

    @Override
    public Supplier<Activity> farmerPlantActivity() {
        return farmerPlantActivity;
    }

    @Override
    public Supplier<Activity> deleverActivity() {
        return deleverActivity;
    }

    @Override
    public Supplier<MenuType<PetBackpackMenu>> petBackpackMenu() {
        return petBackpackMenu;
    }
}
