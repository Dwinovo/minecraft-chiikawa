package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.platform.services.ICapabilityHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

public class NeoForgeCapabilityHelper implements ICapabilityHelper {
    @Override
    public void registerToEventBus(Object eventBus) {
        IEventBus bus = (IEventBus) eventBus;
        bus.addListener(this::registerCapabilities);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerPet(event, InitEntity.USAGI_PET.get());
        registerPet(event, InitEntity.HACHIWARE_PET.get());
        registerPet(event, InitEntity.CHIIKAWA_PET.get());
        registerPet(event, InitEntity.SHISA_PET.get());
        registerPet(event, InitEntity.MOMONGA_PET.get());
        registerPet(event, InitEntity.KURIMANJU_PET.get());
        registerPet(event, InitEntity.RAKKO_PET.get());
        registerPet(event, InitEntity.FURUHONYA_PET.get());
    }

    private static <T extends AbstractPet> void registerPet(RegisterCapabilitiesEvent event, EntityType<T> type) {
        event.registerEntity(Capabilities.Item.ENTITY, type, (pet, context) -> VanillaContainerWrapper.of(pet.getBackpack()));
        event.registerEntity(Capabilities.Item.ENTITY_AUTOMATION, type, (pet, context) -> VanillaContainerWrapper.of(pet.getBackpack()));
    }
}
