package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.handler.ArcherJobHandler;
import com.dwinovo.chiikawa.entity.brain.handler.FarmerJobHandler;
import com.dwinovo.chiikawa.entity.brain.handler.FencerJobHandler;
import com.dwinovo.chiikawa.entity.job.api.IPetJob;
import com.dwinovo.chiikawa.entity.job.impl.BasicJob;
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

    public static final ResourceKey<Registry<IPetJob>> PET_JOB_KEY = ResourceKey.createRegistryKey(
        new ResourceLocation(Constants.MOD_ID, "pet_jobs")
    );

    public static final Registry<IPetJob> PET_JOB_REGISTRY = Services.REGISTRY.createRegistry(
        PET_JOB_KEY,
        new ResourceLocation(Constants.MOD_ID, "none"),
        true
    );

    public static final Supplier<IPetJob> NONE = Services.REGISTRY.register(
        PET_JOB_KEY,
        new ResourceLocation(Constants.MOD_ID, "none"),
        () -> new NoneJob(NONE_ID)
    );
    public static final Supplier<IPetJob> FARMER = Services.REGISTRY.register(
        PET_JOB_KEY,
        new ResourceLocation(Constants.MOD_ID, "farmer"),
        () -> new BasicJob(
            FARMER_ID,
            10,
            InitTag.ENTITY_FARMER_TOOLS,
            FarmerJobHandler::initBrain,
            FarmerJobHandler::tickBrain
        )
    );
    public static final Supplier<IPetJob> FENCER = Services.REGISTRY.register(
        PET_JOB_KEY,
        new ResourceLocation(Constants.MOD_ID, "fencer"),
        () -> new BasicJob(
            FENCER_ID,
            10,
            InitTag.ENTITY_FENCER_TOOLS,
            FencerJobHandler::initBrain,
            FencerJobHandler::tickBrain
        )
    );
    public static final Supplier<IPetJob> ARCHER = Services.REGISTRY.register(
        PET_JOB_KEY,
        new ResourceLocation(Constants.MOD_ID, "archer"),
        () -> new BasicJob(
            ARCHER_ID,
            10,
            InitTag.ENTITY_ARCHER_TOOLS,
            ArcherJobHandler::initBrain,
            ArcherJobHandler::tickBrain
        )
    );

    private InitRegistry() {
    }

    public static void init() {
        // Force class loading before platform event bus registration.
    }

    public static IPetJob getJobFromId(int id) {
        if (id == FARMER_ID) {
            return FARMER.get();
        }
        if (id == FENCER_ID) {
            return FENCER.get();
        }
        if (id == ARCHER_ID) {
            return ARCHER.get();
        }
        return NONE.get();
    }

    public static int getIdFromJob(IPetJob job) {
        if (job == null) {
            return NONE_ID;
        }
        return job.getId();
    }
}
