package com.dwinovo.chiikawa.qualification;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.init.InitLootConditions;
import com.mojang.serialization.Codec;
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
}
