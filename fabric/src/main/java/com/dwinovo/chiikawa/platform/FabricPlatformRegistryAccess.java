package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.sensor.PetAttackbleEntitySensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPlacesSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetFarmerWorkSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetPickableItemSensor;
import com.dwinovo.chiikawa.entity.brain.sensor.PetSocialSensor;
import com.dwinovo.chiikawa.menu.PetBackpackMenu;
import com.dwinovo.chiikawa.platform.services.IPlatformRegistryAccess;
import com.dwinovo.chiikawa.qualification.QualificationCondition;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.world.inventory.MenuType;

public final class FabricPlatformRegistryAccess implements IPlatformRegistryAccess {
    private final Supplier<SensorType<PetAttackbleEntitySensor>> petAttackbleEntitySensor;
    private final Supplier<SensorType<PetFarmerWorkSensor>> petFarmerWorkSensor;
    private final Supplier<SensorType<PetPickableItemSensor>> petItemEntitySensor;
    private final Supplier<SensorType<PetPlacesSensor>> petPlacesSensor;
    private final Supplier<SensorType<PetSocialSensor>> petSocialSensor;
    private final Supplier<MenuType<PetBackpackMenu>> petBackpackMenu;
    private final Supplier<MapCodec<QualificationCondition>> qualificationCondition;

    public FabricPlatformRegistryAccess() {
        MapCodec<QualificationCondition> qualification = Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, id("qualification"),
            QualificationCondition.CODEC);
        qualificationCondition = () -> qualification;
        petAttackbleEntitySensor = registerSensor("pet_attackble_entity_sensor", new SensorType<>(PetAttackbleEntitySensor::new));
        petFarmerWorkSensor = registerSensor("pet_farmer_work_sensor", new SensorType<>(PetFarmerWorkSensor::new));
        petItemEntitySensor = registerSensor("pet_item_entity_sensor", new SensorType<>(PetPickableItemSensor::new));
        petPlacesSensor = registerSensor("pet_places_sensor", new SensorType<>(PetPlacesSensor::new));
        petSocialSensor = registerSensor("pet_social_sensor", new SensorType<>(PetSocialSensor::new));

        petBackpackMenu = registerMenu("pet_backpack", new ExtendedMenuType<>(PetBackpackMenu::new, PetBackpackMenu.OPENING_DATA));
    }

    private static <T extends SensorType<?>> Supplier<T> registerSensor(String path, T type) {
        Registry.register(BuiltInRegistries.SENSOR_TYPE, id(path), type);
        return () -> type;
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
    public Supplier<SensorType<PetPlacesSensor>> petPlacesSensor() {
        return petPlacesSensor;
    }

    @Override
    public Supplier<SensorType<PetSocialSensor>> petSocialSensor() {
        return petSocialSensor;
    }

    @Override
    public Supplier<MapCodec<QualificationCondition>> qualificationCondition() {
        return qualificationCondition;
    }

    @Override
    public Supplier<MenuType<PetBackpackMenu>> petBackpackMenu() {
        return petBackpackMenu;
    }
}
