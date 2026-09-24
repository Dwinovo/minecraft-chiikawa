package com.dwinovo.chiikawa.voice;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * The voices currently loaded from data packs, by entity type id. Server-side only;
 * replaced as a whole on every data pack (re)load.
 */
public final class PetVoices {
    private static volatile Map<Identifier, PetVoice> byEntity = Map.of();

    private PetVoices() {
    }

    /**
     * @param type a pet entity type
     * @return its loaded voice, or {@link PetVoice#SILENT} if none was loaded
     */
    public static PetVoice of(EntityType<?> type) {
        return get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    static PetVoice get(Identifier entityId) {
        return byEntity.getOrDefault(entityId, PetVoice.SILENT);
    }

    static void replaceAll(Map<Identifier, PetVoice> voices) {
        byEntity = Map.copyOf(voices);
    }
}
