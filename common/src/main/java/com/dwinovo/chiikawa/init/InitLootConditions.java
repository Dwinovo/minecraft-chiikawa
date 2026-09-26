package com.dwinovo.chiikawa.init;

import com.dwinovo.chiikawa.platform.Services;
import java.util.function.Supplier;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/** The mod's own loot conditions, for data packs to use in loot tables as vanilla's are. */
public final class InitLootConditions {
    /** {@code chiikawa:qualification}: the pet holds a grade of a licence or better. */
    public static final Supplier<LootItemConditionType> QUALIFICATION =
        Services.PLATFORM_REGISTRY.qualificationCondition();

    private InitLootConditions() {
    }

    public static void init() {
    }
}
