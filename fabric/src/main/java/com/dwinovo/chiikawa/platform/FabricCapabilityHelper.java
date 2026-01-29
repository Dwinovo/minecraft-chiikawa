package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.platform.services.ICapabilityHelper;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class FabricCapabilityHelper implements ICapabilityHelper {
    public static final EntityApiLookup<Storage<ItemVariant>, Void> ENTITY_ITEM_STORAGE =
        EntityApiLookup.get(
            new ResourceLocation(Constants.MOD_ID, "entity_item_storage"),
            Storage.asClass(),
            Void.class
        );

    @Override
    public void registerToEventBus(Object eventBus) {
        registerEntityStorages();
    }

    private static void registerEntityStorages() {
        registerPetStorage(InitEntity.USAGI_PET.get());
        registerPetStorage(InitEntity.HACHIWARE_PET.get());
        registerPetStorage(InitEntity.CHIIKAWA_PET.get());
        registerPetStorage(InitEntity.SHISA_PET.get());
        registerPetStorage(InitEntity.MOMONGA_PET.get());
        registerPetStorage(InitEntity.KURIMANJU_PET.get());
        registerPetStorage(InitEntity.RAKKO_PET.get());
    }

    private static void registerPetStorage(EntityType<? extends AbstractPet> type) {
        ENTITY_ITEM_STORAGE.registerForTypes((entity, context) -> {
            if (entity instanceof AbstractPet pet) {
                return InventoryStorage.of(pet.getBackpack(), null);
            }
            return null;
        }, type);
    }
}
