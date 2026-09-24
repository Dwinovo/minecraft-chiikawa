package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.entity.impl.ChiikawaPet;
import com.dwinovo.chiikawa.entity.impl.FuruhonyaPet;
import com.dwinovo.chiikawa.entity.impl.HachiwarePet;
import com.dwinovo.chiikawa.entity.impl.KurimanjuPet;
import com.dwinovo.chiikawa.entity.impl.MomongaPet;
import com.dwinovo.chiikawa.entity.impl.RakkoPet;
import com.dwinovo.chiikawa.entity.impl.ShisaPet;
import com.dwinovo.chiikawa.entity.impl.UsagiPet;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.platform.services.IEntityHelper;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;

public class FabricEntityHelper implements IEntityHelper {
    @Override
    public void registerToEventBus(Object eventBus) {
        // Fabric doesn't use a mod event bus for entity attributes/spawn placements.
    }

    @Override
    public void registerAttributes() {
        FabricDefaultAttributeRegistry.register(InitEntity.USAGI_PET.get(), UsagiPet.createAttributes().build());
        FabricDefaultAttributeRegistry.register(InitEntity.HACHIWARE_PET.get(), HachiwarePet.createAttributes().build());
        FabricDefaultAttributeRegistry.register(InitEntity.CHIIKAWA_PET.get(), ChiikawaPet.createAttributes().build());
        FabricDefaultAttributeRegistry.register(InitEntity.SHISA_PET.get(), ShisaPet.createAttributes().build());
        FabricDefaultAttributeRegistry.register(InitEntity.MOMONGA_PET.get(), MomongaPet.createAttributes().build());
        FabricDefaultAttributeRegistry.register(InitEntity.KURIMANJU_PET.get(), KurimanjuPet.createAttributes().build());
        FabricDefaultAttributeRegistry.register(InitEntity.RAKKO_PET.get(), RakkoPet.createAttributes().build());
        FabricDefaultAttributeRegistry.register(InitEntity.FURUHONYA_PET.get(), FuruhonyaPet.createAttributes().build());
    }

    @Override
    public void registerSpawnPlacements() {
        registerSpawnPlacement(InitEntity.USAGI_PET.get());
        registerSpawnPlacement(InitEntity.HACHIWARE_PET.get());
        registerSpawnPlacement(InitEntity.CHIIKAWA_PET.get());
        registerSpawnPlacement(InitEntity.SHISA_PET.get());
        registerSpawnPlacement(InitEntity.MOMONGA_PET.get());
        registerSpawnPlacement(InitEntity.KURIMANJU_PET.get());
        registerSpawnPlacement(InitEntity.RAKKO_PET.get());
        registerSpawnPlacement(InitEntity.FURUHONYA_PET.get());
    }

    private static <T extends Animal> void registerSpawnPlacement(net.minecraft.world.entity.EntityType<T> type) {
        // 1.20.1: use SpawnPlacements.Type enum instead of SpawnPlacementTypes
        SpawnPlacements.register(type, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Animal::checkAnimalSpawnRules);
    }

    @Override
    public Entity changeDimension(Entity entity, ServerLevel destination, Vec3 position, float yRot, float xRot) {
        return FabricDimensions.teleport(entity, destination, new PortalInfo(position, Vec3.ZERO, yRot, xRot));
    }
}
