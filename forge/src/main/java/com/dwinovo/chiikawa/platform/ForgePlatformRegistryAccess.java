package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPlacesSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetFarmerWorkSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
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
    private static final RegistryObject<SensorType<PetFarmerWorkSensor>> PET_FARMER_WORK_SENSOR =
        SENSOR_TYPES.register("pet_farmer_work_sensor", () -> new SensorType<>(PetFarmerWorkSensor::new));
    private static final RegistryObject<SensorType<PetPickableItemSensor>> PET_ITEM_ENTITY_SENSOR =
        SENSOR_TYPES.register("pet_item_entity_sensor", () -> new SensorType<>(PetPickableItemSensor::new));
    private static final RegistryObject<SensorType<PetPlacesSensor>> PET_PLACES_SENSOR =
        SENSOR_TYPES.register("pet_places_sensor", () -> new SensorType<>(PetPlacesSensor::new));

    // Activity registrations
    private static final RegistryObject<Activity> FARMER_HARVEST =
        ACTIVITIES.register("farmer_harvest", () -> new Activity("farmer_harvest"));
    private static final RegistryObject<Activity> FARMER_PLANT =
        ACTIVITIES.register("farmer_plant", () -> new Activity("farmer_plant"));
    private static final RegistryObject<Activity> DELEVER =
        ACTIVITIES.register("delever", () -> new Activity("delever"));
    private static final RegistryObject<Activity> WEED =
        ACTIVITIES.register("weed", () -> new Activity("weed"));
    private static final RegistryObject<Activity> PICK_MUSHROOM =
        ACTIVITIES.register("pick_mushroom", () -> new Activity("pick_mushroom"));
    private static final RegistryObject<Activity> FENCER_FIGHT =
        ACTIVITIES.register("fencer_fight", () -> new Activity("fencer_fight"));
    private static final RegistryObject<Activity> ARCHER_SHOOT =
        ACTIVITIES.register("archer_shoot", () -> new Activity("archer_shoot"));
    private static final RegistryObject<Activity> MUSICIAN_PLAY =
        ACTIVITIES.register("musician_play", () -> new Activity("musician_play"));
    private static final RegistryObject<Activity> FOLLOW_OWNER =
        ACTIVITIES.register("follow_owner", () -> new Activity("follow_owner"));
    private static final RegistryObject<Activity> STAY =
        ACTIVITIES.register("stay", () -> new Activity("stay"));
    private static final RegistryObject<Activity> PICK_UP =
        ACTIVITIES.register("pick_up", () -> new Activity("pick_up"));
    private static final RegistryObject<Activity> TAKE_TASK =
        ACTIVITIES.register("take_task", () -> new Activity("take_task"));
    private static final RegistryObject<Activity> SHOP =
        ACTIVITIES.register("shop", () -> new Activity("shop"));

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
    public Supplier<SensorType<PetFarmerWorkSensor>> petFarmerWorkSensor() {
        return PET_FARMER_WORK_SENSOR;
    }

    @Override
    public Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor() {
        return PET_ITEM_ENTITY_SENSOR;
    }

    @Override
    public Supplier<SensorType<PetPlacesSensor>> petPlacesSensor() {
        return PET_PLACES_SENSOR;
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
    public Supplier<Activity> weedActivity() {
        return WEED;
    }

    @Override
    public Supplier<Activity> pickMushroomActivity() {
        return PICK_MUSHROOM;
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
    public Supplier<Activity> musicianPlayActivity() {
        return MUSICIAN_PLAY;
    }

    @Override
    public Supplier<Activity> followOwnerActivity() {
        return FOLLOW_OWNER;
    }

    @Override
    public Supplier<Activity> stayActivity() {
        return STAY;
    }

    @Override
    public Supplier<Activity> pickUpActivity() {
        return PICK_UP;
    }

    @Override
    public Supplier<Activity> takeTaskActivity() {
        return TAKE_TASK;
    }

    @Override
    public Supplier<Activity> shopActivity() {
        return SHOP;
    }

    @Override
    public Supplier<MenuType<PetBackpackMenu>> petBackpackMenu() {
        return PET_BACKPACK;
    }
}
