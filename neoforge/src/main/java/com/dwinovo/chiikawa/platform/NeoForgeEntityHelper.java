package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.entity.impl.ChiikawaPet;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import com.dwinovo.chiikawa.entity.impl.KurimanjuPet;
import com.dwinovo.chiikawa.entity.impl.MomongaPet;
import com.dwinovo.chiikawa.entity.impl.RakkoPet;
import com.dwinovo.chiikawa.entity.impl.ShisaPet;
import com.dwinovo.chiikawa.entity.impl.UsagiPet;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.platform.services.IEntityHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.SpawnPlacementRegisterEvent;

public class NeoForgeEntityHelper implements IEntityHelper {
    @Override
    public void registerToEventBus(Object eventBus) {
        IEventBus bus = (IEventBus) eventBus;
        bus.addListener((EntityAttributeCreationEvent event) -> registerAttributes(event));
        bus.addListener((SpawnPlacementRegisterEvent event) -> registerSpawnPlacements(event));
    }

    @Override
    public void registerAttributes() {
        // No-op; NeoForge uses events.
    }

    @Override
    public void registerSpawnPlacements() {
        // No-op; NeoForge uses events.
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(InitEntity.USAGI_PET.get(), UsagiPet.createAttributes().build());
        event.put(InitEntity.HACHIWARE_PET.get(), HachiwarePet.createAttributes().build());
        event.put(InitEntity.CHIIKAWA_PET.get(), ChiikawaPet.createAttributes().build());
        event.put(InitEntity.SHISA_PET.get(), ShisaPet.createAttributes().build());
        event.put(InitEntity.MOMONGA_PET.get(), MomongaPet.createAttributes().build());
        event.put(InitEntity.KURIMANJU_PET.get(), KurimanjuPet.createAttributes().build());
        event.put(InitEntity.RAKKO_PET.get(), RakkoPet.createAttributes().build());
    }

    private void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        registerSpawnPlacement(event, InitEntity.USAGI_PET.get());
        registerSpawnPlacement(event, InitEntity.HACHIWARE_PET.get());
        registerSpawnPlacement(event, InitEntity.CHIIKAWA_PET.get());
        registerSpawnPlacement(event, InitEntity.SHISA_PET.get());
        registerSpawnPlacement(event, InitEntity.MOMONGA_PET.get());
        registerSpawnPlacement(event, InitEntity.KURIMANJU_PET.get());
        registerSpawnPlacement(event, InitEntity.RAKKO_PET.get());
    }

    private static <T extends Animal> void registerSpawnPlacement(SpawnPlacementRegisterEvent event, EntityType<T> entity) {
        event.register(entity, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Animal::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.OR);
    }
}
