package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitLootConditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * {@code chiikawa:qualification}: whether the pet a loot table is rolled for holds a grade
 * of a licence or better, as vanilla's own conditions ask what an entity is or holds. A
 * slip's reward is rolled with the pet as {@code this_entity}, so a reward can pay a
 * licensed pet more without the code that pays it knowing why.
 */
public record QualificationCondition(LicenceRequirement requirement) implements LootItemCondition {
    public static final Codec<QualificationCondition> CODEC =
        LicenceRequirement.CODEC.xmap(QualificationCondition::new, QualificationCondition::requirement);

    @Override
    public LootItemConditionType getType() {
        return InitLootConditions.QUALIFICATION.get();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    public boolean test(LootContext context) {
        Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        return entity instanceof AbstractPet pet && requirement.metBy(pet.licences());
    }

    /** 1.20.1 reads and writes a condition through a serializer rather than a codec; this one hands both to {@link #CODEC}. */
    public static final class Serializer implements net.minecraft.world.level.storage.loot.Serializer<QualificationCondition> {
        @Override
        public void serialize(JsonObject json, QualificationCondition condition, JsonSerializationContext context) {
            CODEC.encodeStart(JsonOps.INSTANCE, condition).getOrThrow(false, Constants.LOG::error)
                .getAsJsonObject().entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
        }

        @Override
        public QualificationCondition deserialize(JsonObject json, JsonDeserializationContext context) {
            return CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, Constants.LOG::error);
        }
    }
}
