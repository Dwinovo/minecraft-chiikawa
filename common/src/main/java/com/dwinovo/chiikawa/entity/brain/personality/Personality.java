package com.dwinovo.chiikawa.entity.brain.personality;

import com.dwinovo.chiikawa.entity.brain.intent.DayPhase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * How one kind of pet leans: how much it wants to do each intent, how that shifts over
 * the day, how random its choices are, and what it holds when it spawns in the wild.
 * Loaded per entity type from {@code data/<namespace>/pet_personality/<entity>.json}
 * by {@link PetPersonalityLoader}.
 *
 * <p>Multipliers only weigh intents the owner's directive already permits; a
 * personality can never make a pet do what it was told not to.
 *
 * @param intentMultipliers score multiplier per intent id; unlisted intents keep 1
 * @param routine extra score multipliers per intent id for a part of the day
 * @param randomness how far the pet's choices stray from the best score, from 0 to 1;
 *                   see {@code IntentSelector#choose}
 * @param wildTools the tools a wild pet of this kind may spawn holding, by weight;
 *                  {@code minecraft:air} weighs spawning empty-handed
 */
public record Personality(
    Map<ResourceLocation, Float> intentMultipliers,
    Map<DayPhase, Map<ResourceLocation, Float>> routine,
    float randomness,
    List<WildTool> wildTools
) {
    /** No leanings: every multiplier 1, no randomness, and wild pets spawn empty-handed. */
    public static final Personality DEFAULT = new Personality(Map.of(), Map.of(), 0.0F, List.of());

    private static final Codec<Map<ResourceLocation, Float>> MULTIPLIERS_CODEC =
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.floatRange(0.0F, Float.MAX_VALUE));

    public static final Codec<Personality> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        MULTIPLIERS_CODEC.optionalFieldOf("intent_multipliers", Map.of()).forGetter(Personality::intentMultipliers),
        Codec.unboundedMap(DayPhase.CODEC, MULTIPLIERS_CODEC).optionalFieldOf("routine", Map.of()).forGetter(Personality::routine),
        Codec.floatRange(0.0F, 1.0F).optionalFieldOf("randomness", DEFAULT.randomness()).forGetter(Personality::randomness),
        WildTool.CODEC.listOf().optionalFieldOf("wild_tools", List.of()).forGetter(Personality::wildTools)
    ).apply(instance, Personality::new));

    public Personality {
        intentMultipliers = Map.copyOf(intentMultipliers);
        routine = routine.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
        wildTools = List.copyOf(wildTools);
    }

    /**
     * @param intent an intent id
     * @param phase the current part of the day
     * @return the factor this pet's base score for {@code intent} is multiplied by
     */
    public float multiplier(ResourceLocation intent, DayPhase phase) {
        return intentMultipliers.getOrDefault(intent, 1.0F)
            * routine.getOrDefault(phase, Map.of()).getOrDefault(intent, 1.0F);
    }

    /** @return every intent id this personality weighs, for validation */
    public Set<ResourceLocation> intentIds() {
        Set<ResourceLocation> ids = new HashSet<>(intentMultipliers.keySet());
        routine.values().forEach(multipliers -> ids.addAll(multipliers.keySet()));
        return ids;
    }

    /**
     * @param random the spawn's random source
     * @return the tool a wild pet of this kind spawns holding, empty when it spawns empty-handed
     */
    public ItemStack drawWildTool(RandomSource random) {
        return WeightedRandom.getRandomItem(random, wildTools)
            .map(tool -> new ItemStack(tool.item()))
            .orElse(ItemStack.EMPTY);
    }

    /**
     * One weighted choice of {@link #wildTools}.
     */
    public record WildTool(Item item, Weight weight) implements WeightedEntry {
        /** Lazy because the item registry only exists once the game has bootstrapped. */
        public static final Codec<WildTool> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(WildTool::item),
            ExtraCodecs.POSITIVE_INT.xmap(Weight::of, Weight::asInt).fieldOf("weight").forGetter(WildTool::weight)
        ).apply(instance, WildTool::new)));

        public WildTool(Item item, int weight) {
            this(item, Weight.of(weight));
        }

        @Override
        public Weight getWeight() {
            return weight;
        }
    }
}
