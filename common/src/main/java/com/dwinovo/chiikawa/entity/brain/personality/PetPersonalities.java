package com.dwinovo.chiikawa.entity.brain.personality;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * The personalities currently loaded from data packs, by entity type id. Server-side
 * only; replaced as a whole on every data pack (re)load.
 */
public final class PetPersonalities {
    private static volatile Map<Identifier, Personality> byEntity = Map.of();

    private PetPersonalities() {
    }

    /**
     * @param type a pet entity type
     * @return its loaded personality, or {@link Personality#DEFAULT} if none was loaded
     */
    public static Personality of(EntityType<?> type) {
        return get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    static Personality get(Identifier entityId) {
        return byEntity.getOrDefault(entityId, Personality.DEFAULT);
    }

    static void replaceAll(Map<Identifier, Personality> personalities) {
        byEntity = Map.copyOf(personalities);
    }
}
