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
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public final class FabricPlatformRegistryAccess implements IPlatformRegistryAccess {
    private final Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor;
    private final Supplier<SensorType<PetFarmerWorkSensor>> petFarmerWorkSensor;
    private final Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor;
    private final Supplier<SensorType<PetPlacesSensor>> petPlacesSensor;
    private final Supplier<SensorType<PetSocialSensor>> petSocialSensor;
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
    private final Supplier<Activity> shopActivity;
    private final Supplier<Activity> giftOwnerActivity;
    private final Supplier<Activity> socializeActivity;
    private final Supplier<Activity> cooperateActivity;
    private final Supplier<Activity> takeExamActivity;
    private final Supplier<Activity> checkResultsActivity;
    private final Supplier<Activity> answerWhistleActivity;
    private final Supplier<MenuType<PetBackpackMenu>> petBackpackMenu;
    private final Supplier<LootItemConditionType> qualificationCondition;

    public FabricPlatformRegistryAccess() {
        LootItemConditionType qualification = Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, id("qualification"),
            new LootItemConditionType(new QualificationCondition.Serializer()));
        qualificationCondition = () -> qualification;
        petAttackbleEntitySensor = registerSensor("pet_attackble_entity_sensor", new SensorType<>(PetAttackbleEntitySensor::new));
        petFarmerWorkSensor = registerSensor("pet_farmer_work_sensor", new SensorType<>(PetFarmerWorkSensor::new));
        petItemEntitySensor = registerSensor("pet_item_entity_sensor", new SensorType<>(PetPickableItemSensor::new));
        petPlacesSensor = registerSensor("pet_places_sensor", new SensorType<>(PetPlacesSensor::new));
        petSocialSensor = registerSensor("pet_social_sensor", new SensorType<>(PetSocialSensor::new));

        farmerHarvestActivity = registerActivity("farmer_harvest");
        farmerPlantActivity = registerActivity("farmer_plant");
        deleverActivity = registerActivity("delever");
        weedActivity = registerActivity("weed");
        pickMushroomActivity = registerActivity("pick_mushroom");
        fencerFightActivity = registerActivity("fencer_fight");
        archerShootActivity = registerActivity("archer_shoot");
        musicianPlayActivity = registerActivity("musician_play");
        followOwnerActivity = registerActivity("follow_owner");
        stayActivity = registerActivity("stay");
        pickUpActivity = registerActivity("pick_up");
        takeTaskActivity = registerActivity("take_task");
        shopActivity = registerActivity("shop");
        giftOwnerActivity = registerActivity("gift_owner");
        socializeActivity = registerActivity("socialize");
        cooperateActivity = registerActivity("cooperate");
        takeExamActivity = registerActivity("take_exam");
        checkResultsActivity = registerActivity("check_results");
        answerWhistleActivity = registerActivity("answer_whistle");

        petBackpackMenu = registerMenu("pet_backpack", new MenuType<>(PetBackpackMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    }

    private static <T extends SensorType<?>> Supplier<T> registerSensor(String path, T type) {
        Registry.register(BuiltInRegistries.SENSOR_TYPE, id(path), type);
        return () -> type;
    }

    private static Supplier<Activity> registerActivity(String path) {
        Activity activity = Registry.register(BuiltInRegistries.ACTIVITY, id(path), new Activity(PetActivities.name(path)));
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
    public Supplier<SensorType<PetFarmerWorkSensor>> petFarmerWorkSensor() {
        return petFarmerWorkSensor;
    }

    @Override
    public Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor() {
        return petItemEntitySensor;
    }

    @Override
    public Supplier<SensorType<PetPlacesSensor>> petPlacesSensor() {
        return petPlacesSensor;
    }

    @Override
    public Supplier<SensorType<PetSocialSensor>> petSocialSensor() {
        return petSocialSensor;
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
    public Supplier<Activity> shopActivity() {
        return shopActivity;
    }

    @Override
    public Supplier<Activity> giftOwnerActivity() {
        return giftOwnerActivity;
    }

    @Override
    public Supplier<Activity> socializeActivity() {
        return socializeActivity;
    }

    @Override
    public Supplier<Activity> cooperateActivity() {
        return cooperateActivity;
    }

    @Override
    public Supplier<LootItemConditionType> qualificationCondition() {
        return qualificationCondition;
    }

    @Override
    public Supplier<Activity> takeExamActivity() {
        return takeExamActivity;
    }

    @Override
    public Supplier<Activity> checkResultsActivity() {
        return checkResultsActivity;
    }

    @Override
    public Supplier<Activity> answerWhistleActivity() {
        return answerWhistleActivity;
    }

    @Override
    public Supplier<MenuType<PetBackpackMenu>> petBackpackMenu() {
        return petBackpackMenu;
    }
}
