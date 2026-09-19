package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetBoardSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetFarmerWorkSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.platform.services.IPlatformRegistryAccess;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MenuType;

public final class FabricPlatformRegistryAccess implements IPlatformRegistryAccess {
    private final Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor;
    private final Supplier<SensorType<PetFarmerWorkSensor>> petFarmerWorkSensor;
    private final Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor;
    private final Supplier<SensorType<PetBoardSensor>> petBoardSensor;
    private final Supplier<Activity> farmerHarvestActivity;
    private final Supplier<Activity> farmerPlantActivity;
    private final Supplier<Activity> deleverActivity;
    private final Supplier<Activity> weedActivity;
    private final Supplier<Activity> pickMushroomActivity;
    private final Supplier<Activity> fencerFightActivity;
    private final Supplier<Activity> archerShootActivity;
    private final Supplier<Activity> musicianPlayActivity;
    private final Supplier<Activity> followOwnerActivity;
    private final Supplier<Activity> stayActivity;
    private final Supplier<Activity> pickUpActivity;
    private final Supplier<Activity> takeTaskActivity;
    private final Supplier<MenuType<PetBackpackMenu>> petBackpackMenu;

    public FabricPlatformRegistryAccess() {
        petAttackbleEntitySensor = registerSensor("pet_attackble_entity_sensor", new SensorType<>(PetAttackbleEntitySensor::new));
        petFarmerWorkSensor = registerSensor("pet_farmer_work_sensor", new SensorType<>(PetFarmerWorkSensor::new));
        petItemEntitySensor = registerSensor("pet_item_entity_sensor", new SensorType<>(PetPickableItemSensor::new));
        petBoardSensor = registerSensor("pet_board_sensor", new SensorType<>(PetBoardSensor::new));

        farmerHarvestActivity = registerActivity("farmer_harvest", new Activity("farmer_harvest"));
        farmerPlantActivity = registerActivity("farmer_plant", new Activity("farmer_plant"));
        deleverActivity = registerActivity("delever", new Activity("delever"));
        weedActivity = registerActivity("weed", new Activity("weed"));
        pickMushroomActivity = registerActivity("pick_mushroom", new Activity("pick_mushroom"));
        fencerFightActivity = registerActivity("fencer_fight", new Activity("fencer_fight"));
        archerShootActivity = registerActivity("archer_shoot", new Activity("archer_shoot"));
        musicianPlayActivity = registerActivity("musician_play", new Activity("musician_play"));
        followOwnerActivity = registerActivity("follow_owner", new Activity("follow_owner"));
        stayActivity = registerActivity("stay", new Activity("stay"));
        pickUpActivity = registerActivity("pick_up", new Activity("pick_up"));
        takeTaskActivity = registerActivity("take_task", new Activity("take_task"));

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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    @Override
    public Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor() {
        return petAttackbleEntitySensor;
    }

    @Override
    public Supplier<SensorType<PetFarmerWorkSensor>> petFarmerWorkSensor() {
        return petFarmerWorkSensor;
    }

    @Override
    public Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor() {
        return petItemEntitySensor;
    }

    @Override
    public Supplier<SensorType<PetBoardSensor>> petBoardSensor() {
        return petBoardSensor;
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
    public Supplier<Activity> weedActivity() {
        return weedActivity;
    }

    @Override
    public Supplier<Activity> pickMushroomActivity() {
        return pickMushroomActivity;
    }

    @Override
    public Supplier<Activity> fencerFightActivity() {
        return fencerFightActivity;
    }

    @Override
    public Supplier<Activity> archerShootActivity() {
        return archerShootActivity;
    }

    @Override
    public Supplier<Activity> musicianPlayActivity() {
        return musicianPlayActivity;
    }

    @Override
    public Supplier<Activity> followOwnerActivity() {
        return followOwnerActivity;
    }

    @Override
    public Supplier<Activity> stayActivity() {
        return stayActivity;
    }

    @Override
    public Supplier<Activity> pickUpActivity() {
        return pickUpActivity;
    }

    @Override
    public Supplier<Activity> takeTaskActivity() {
        return takeTaskActivity;
    }

    @Override
    public Supplier<MenuType<PetBackpackMenu>> petBackpackMenu() {
        return petBackpackMenu;
    }
}
