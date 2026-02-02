package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.platform.capability.IPetBackpackHandler;
import com.dwinovo.chiikawa.platform.capability.PetBackpackHandler;
import com.dwinovo.chiikawa.platform.services.ICapabilityHelper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.items.IItemHandler;

public class ForgeCapabilityHelper implements ICapabilityHelper {
    private static final ResourceLocation ITEM_HANDLER_CAP = new ResourceLocation("chiikawa", "item_handler");
    private static final ResourceLocation PET_BACKPACK_CAP = new ResourceLocation("chiikawa", "pet_backpack");
    public static final Capability<IPetBackpackHandler> PET_BACKPACK_CAPABILITY =
        CapabilityManager.get(new CapabilityToken<>() {});

    @Override
    public void registerToEventBus(Object eventBus) {
        // Register on the Forge event bus for capability attachment
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::onAttachCapabilities);
        if (eventBus instanceof IEventBus bus) {
            bus.addListener(this::onRegisterCapabilities);
        }
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPetBackpackHandler.class);
    }

    private void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof AbstractPet pet) {
            LazyOptional<IPetBackpackHandler> petHandler =
                LazyOptional.of(() -> new PetBackpackHandler(pet.getBackpack()));
            LazyOptional<IItemHandler> itemHandler = petHandler.cast();
            event.addCapability(ITEM_HANDLER_CAP, new ICapabilityProvider() {
                @Override
                @Nonnull
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                    return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap, itemHandler);
                }
            });
            event.addCapability(PET_BACKPACK_CAP, new ICapabilityProvider() {
                @Override
                @Nonnull
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                    return PET_BACKPACK_CAPABILITY.orEmpty(cap, petHandler);
                }
            });
            event.addListener(petHandler::invalidate);
        }
    }
}
