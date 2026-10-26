package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.job.JobIds;
import com.dwinovo.chiikawa.entity.job.api.PetCapability;
import com.dwinovo.chiikawa.entity.job.impl.BasicJob;
import com.dwinovo.chiikawa.entity.job.impl.MusicianJob;
import com.dwinovo.chiikawa.entity.job.impl.NoneJob;
import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class InitRegistry {
    public static final int NONE_ID = 0;
    public static final int FARMER_ID = 1;
    public static final int FENCER_ID = 2;
    public static final int ARCHER_ID = 3;
    public static final int MUSICIAN_ID = 4;

    public static final ResourceKey<Registry<PetCapability>> PET_JOB_KEY = ResourceKey.createRegistryKey(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "pet_jobs")
    );

    public static final Registry<PetCapability> PET_JOB_REGISTRY = Services.REGISTRY.createRegistry(
        PET_JOB_KEY,
        JobIds.NONE,
        true
    );

    public static final Supplier<PetCapability> NONE = Services.REGISTRY.register(
        PET_JOB_REGISTRY,
        JobIds.NONE,
        () -> new NoneJob(NONE_ID)
    );
    public static final Supplier<PetCapability> FARMER = Services.REGISTRY.register(
        PET_JOB_REGISTRY,
        JobIds.FARMER,
        () -> new BasicJob(
            FARMER_ID,
            10,
            InitTag.ENTITY_FARMER_TOOLS
        )
    );
    public static final Supplier<PetCapability> FENCER = Services.REGISTRY.register(
        PET_JOB_REGISTRY,
        JobIds.FENCER,
        () -> new BasicJob(
            FENCER_ID,
            10,
            InitTag.ENTITY_FENCER_TOOLS
        )
    );
    public static final Supplier<PetCapability> ARCHER = Services.REGISTRY.register(
        PET_JOB_REGISTRY,
        JobIds.ARCHER,
        () -> new BasicJob(
            ARCHER_ID,
            10,
            InitTag.ENTITY_ARCHER_TOOLS
        )
    );
    public static final Supplier<PetCapability> MUSICIAN = Services.REGISTRY.register(
        PET_JOB_REGISTRY,
        JobIds.MUSICIAN,
        () -> new MusicianJob(
            MUSICIAN_ID,
            10,
            InitTag.ENTITY_MUSICIAN_TOOLS
        )
    );

    private InitRegistry() {
    }

    public static void init() {
        // Force class loading before platform event bus registration.
    }

    public static PetCapability getCapabilityFromId(int id) {
        if (id == FARMER_ID) {
            return FARMER.get();
        }
        if (id == FENCER_ID) {
            return FENCER.get();
        }
        if (id == ARCHER_ID) {
            return ARCHER.get();
        }
        if (id == MUSICIAN_ID) {
            return MUSICIAN.get();
        }
        return NONE.get();
    }
}
