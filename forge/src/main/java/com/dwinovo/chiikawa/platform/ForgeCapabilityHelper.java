package com.dwinovo.chiikawa.platform;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitEntity;
import com.dwinovo.chiikawa.platform.services.ICapabilityHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraft.core.Direction;
import net.minecraftforge.common.MinecraftForge;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ForgeCapabilityHelper implements ICapabilityHelper {
    private static final ResourceLocation ITEM_HANDLER_CAP = new ResourceLocation("chiikawa", "item_handler");

    @Override
    public void registerToEventBus(Object eventBus) {
        // Register on the Forge event bus for capability attachment
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, this::onAttachCapabilities);
    }

    private void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof AbstractPet pet) {
            LazyOptional<IItemHandler> handler = LazyOptional.of(() -> new InvWrapper(pet.getBackpack()));
            event.addCapability(ITEM_HANDLER_CAP, new ICapabilityProvider() {
                @Override
                @Nonnull
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                    if (cap == ForgeCapabilities.ITEM_HANDLER) {
                        return handler.cast();
                    }
                    return LazyOptional.empty();
                }
            });
        }
    }
}
