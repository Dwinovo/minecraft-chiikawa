package com.dwinovo.chiikawa.compat.jade;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ChiikawaJadePlugin implements IWailaPlugin {
    public static final ResourceLocation PET_JOB_UID = new ResourceLocation("chiikawa", "pet_job");

    @Override
    public void register(IWailaCommonRegistration registration) {
        // No server data needed; PET_JOB is synced by SynchedEntityData.
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(PetJobComponentProvider.INSTANCE, AbstractPet.class);
    }
}


