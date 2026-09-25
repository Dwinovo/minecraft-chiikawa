package com.dwinovo.chiikawa.entity.brain.personality;

import com.dwinovo.chiikawa.entity.brain.intent.DayPhase;
import com.dwinovo.chiikawa.utils.ModCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
 * @param likes what this kind of pet would spend its own money on, by weight. A pet with
 *              nothing listed buys nothing: a shop is somewhere it goes because it wants
 *              something, not a chore it performs
 * @param idle what it does with itself when it has nothing to do
 */
public record Personality(
    Map<ResourceLocation, Float> intentMultipliers,
    Map<DayPhase, Map<ResourceLocation, Float>> routine,
    float randomness,
    List<WeightedItem> wildTools,
    List<WeightedItem> likes,
    IdleHabits idle
) {
    /** No leanings: every multiplier 1, no randomness, nothing held, nothing wanted, no habits of its own. */
    public static final Personality DEFAULT = new Personality(Map.of(), Map.of(), 0.0F, List.of(), List.of(),
        IdleHabits.DEFAULT);

    private static final Codec<Map<ResourceLocation, Float>> MULTIPLIERS_CODEC =
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.floatRange(0.0F, Float.MAX_VALUE));

    public static final Codec<Personality> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.strictOptionalField(MULTIPLIERS_CODEC, "intent_multipliers", Map.of()).forGetter(Personality::intentMultipliers),
        ExtraCodecs.strictOptionalField(Codec.unboundedMap(DayPhase.CODEC, MULTIPLIERS_CODEC), "routine", Map.of()).forGetter(Personality::routine),
        ExtraCodecs.strictOptionalField(Codec.floatRange(0.0F, 1.0F), "randomness", DEFAULT.randomness()).forGetter(Personality::randomness),
        ExtraCodecs.strictOptionalField(WeightedItem.CODEC.listOf(), "wild_tools", List.of()).forGetter(Personality::wildTools),
        ExtraCodecs.strictOptionalField(WeightedItem.CODEC.listOf(), "likes", List.of()).forGetter(Personality::likes),
        IdleHabits.CODEC.optionalFieldOf("idle", IdleHabits.DEFAULT).forGetter(Personality::idle)
    ).apply(instance, Personality::new));

    public Personality {
        intentMultipliers = Map.copyOf(intentMultipliers);
        routine = routine.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
        wildTools = List.copyOf(wildTools);
        likes = List.copyOf(likes);
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
     * What a pet would buy for itself, drawn the same way as what it spawns holding.
     *
     * @param random this pet's random source
     * @return one of its likings, or nothing when this kind of pet wants for nothing
     */
    public Optional<Item> drawLiking(RandomSource random) {
        return WeightedRandom.getRandomItem(random, likes).map(WeightedItem::item);
    }

    /**
     * An item with a weight: one entry of a list a pet draws from. Both the tools a wild
     * pet turns up holding and the things it would buy are drawn the same way, so they are
     * the same shape.
     */
    public record WeightedItem(Item item, Weight weight) implements WeightedEntry {
        /** Lazy because the item registry only exists once the game has bootstrapped. */
        public static final Codec<WeightedItem> CODEC = ExtraCodecs.lazyInitializedCodec(() -> RecordCodecBuilder.create(instance -> instance.group(
            ModCodecs.ITEM.fieldOf("item").forGetter(WeightedItem::item),
            ExtraCodecs.POSITIVE_INT.xmap(Weight::of, Weight::asInt).fieldOf("weight").forGetter(WeightedItem::weight)
        ).apply(instance, WeightedItem::new)));

        public WeightedItem(Item item, int weight) {
            this(item, Weight.of(weight));
        }

        @Override
        public Weight getWeight() {
            return weight;
        }
    }
}
