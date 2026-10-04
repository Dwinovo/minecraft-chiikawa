package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import com.dwinovo.chiikawa.qualification.QualificationCondition;
import com.mojang.serialization.MapCodec;

/** The mod's own loot conditions, for data packs to use in loot tables as vanilla's are. */
public final class InitLootConditions {
    /** {@code chiikawa:qualification}: the pet holds a grade of a licence or better. */
    public static final Supplier<MapCodec<QualificationCondition>> QUALIFICATION =
        Services.PLATFORM_REGISTRY.qualificationCondition();

    private InitLootConditions() {
    }

    public static void init() {
    }
}
