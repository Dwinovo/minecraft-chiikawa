package com.dwinovo.chiikawa.task;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A kind of slip a labor board can put up, loaded from
 * {@code data/<namespace>/pet_task/<id>.json} by {@link PetTaskTypeLoader}.
 *
 * @param capability the capability (job) id of the pets that can take it
 * @param counter the work counter that advances it, see {@link PetWorkCounters}
 * @param icon the item a board and a screen picture this work as, so a slip is known by
 *             its picture before its name is read
 * @param amount how much of that work a rolled slip asks for
 * @param reward loot table rolled into the pet's backpack once a slip is finished
 * @param weight how often a board puts up this type relative to the others
 * @param minLevel the board level this type needs before it goes up at all, so an
 *                 upgrade is worth paying for rather than only worth counting
 */
public record PetTaskType(
    Identifier capability,
    Identifier counter,
    Identifier icon,
    IntProvider amount,
    ResourceKey<LootTable> reward,
    int weight,
    int minLevel
) {
    public static final Codec<PetTaskType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("capability").forGetter(PetTaskType::capability),
        Identifier.CODEC.fieldOf("counter").forGetter(PetTaskType::counter),
        Identifier.CODEC.fieldOf("icon").forGetter(PetTaskType::icon),
        IntProviders.POSITIVE_CODEC.fieldOf("amount").forGetter(PetTaskType::amount),
        ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("reward").forGetter(PetTaskType::reward),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(PetTaskType::weight),
        ExtraCodecs.POSITIVE_INT.optionalFieldOf("min_level", BoardLevels.FIRST_LEVEL).forGetter(PetTaskType::minLevel)
    ).apply(instance, PetTaskType::new));

    /**
     * @param id this type's id
     * @param random the board's roll
     * @return a fresh slip of this type
     */
    public PetTask roll(Identifier id, RandomSource random) {
        return new PetTask(id, capability, counter, icon, amount.sample(random), reward, 0);
    }
}
