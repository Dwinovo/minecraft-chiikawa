package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.entity.brain.PetActivities;
import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPlacesSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetFarmerWorkSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetSocialSensor;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.platform.services.IPlatformRegistryAccess;
import com.dwinovo.chiikawa.qualification.QualificationCondition;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

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
    private static final DeferredRegister<LootItemConditionType> LOOT_CONDITIONS =
        DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Constants.MOD_ID);

    private static final RegistryObject<LootItemConditionType> QUALIFICATION_CONDITION =
        LOOT_CONDITIONS.register("qualification", () -> new LootItemConditionType(new QualificationCondition.Serializer()));

    // Sensor registrations
    private static final RegistryObject<SensorType<PetAttackbleEntitySensor>> PET_ATTACKBLE_ENTITY_SENSOR =
        SENSOR_TYPES.register("pet_attackble_entity_sensor", () -> new SensorType<>(PetAttackbleEntitySensor::new));
    private static final RegistryObject<SensorType<PetFarmerWorkSensor>> PET_FARMER_WORK_SENSOR =
        SENSOR_TYPES.register("pet_farmer_work_sensor", () -> new SensorType<>(PetFarmerWorkSensor::new));
    private static final RegistryObject<SensorType<PetPickableItemSensor>> PET_ITEM_ENTITY_SENSOR =
        SENSOR_TYPES.register("pet_item_entity_sensor", () -> new SensorType<>(PetPickableItemSensor::new));
    private static final RegistryObject<SensorType<PetPlacesSensor>> PET_PLACES_SENSOR =
        SENSOR_TYPES.register("pet_places_sensor", () -> new SensorType<>(PetPlacesSensor::new));
    private static final RegistryObject<SensorType<PetSocialSensor>> PET_SOCIAL_SENSOR =
        SENSOR_TYPES.register("pet_social_sensor", () -> new SensorType<>(PetSocialSensor::new));

    private static final RegistryObject<Activity> ANSWER_WHISTLE =
        ACTIVITIES.register("answer_whistle", () -> new Activity(PetActivities.name("answer_whistle")));

    // Menu registrations
    private static final RegistryObject<MenuType<PetBackpackMenu>> PET_BACKPACK =
        MENUS.register("pet_backpack", () -> IForgeMenuType.create((containerId, inventory, buf) ->
            new PetBackpackMenu(containerId, inventory, PetBackpackMenu.readOpeningData(buf))
        ));

    public static void register(IEventBus modEventBus) {
        // Register NewRegistryEvent listener to capture custom registry
        modEventBus.addListener(ForgePlatformRegistryAccess::onNewRegistry);
        
        // Register all DeferredRegisters
        SENSOR_TYPES.register(modEventBus);
        ACTIVITIES.register(modEventBus);
        MENUS.register(modEventBus);
        LOOT_CONDITIONS.register(modEventBus);
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
    public Supplier<SensorType<PetSocialSensor>> petSocialSensor() {
        return PET_SOCIAL_SENSOR;
    }

    @Override
    public Supplier<LootItemConditionType> qualificationCondition() {
        return QUALIFICATION_CONDITION;
    }

    @Override
    public Supplier<Activity> answerWhistleActivity() {
        return ANSWER_WHISTLE;
    }

    @Override
    public Supplier<MenuType<PetBackpackMenu>> petBackpackMenu() {
        return PET_BACKPACK;
    }
}
