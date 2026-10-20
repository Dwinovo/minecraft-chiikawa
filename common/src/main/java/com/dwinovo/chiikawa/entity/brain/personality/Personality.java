package com.dwinovo.chiikawa.entity.brain.personality;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
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
 * @param randomness how far the pet's choices stray from the best score among intents of the
 *                   same priority, from 0 to 1; see {@code Decision#pickWithinTier}
 * @param wildTools the tools a wild pet of this kind may spawn holding, by weight;
 *                  {@code minecraft:air} weighs spawning empty-handed
 * @param likes what this kind of pet would spend its own money on, by weight. A pet with
 *              nothing listed buys nothing: a shop is somewhere it goes because it wants
 *              something, not a chore it performs
 * @param idle what it does with itself when it has nothing to do
 * @param qualifications how it takes to each licence's exams, by licence id; a licence
 *                       left out gets {@link Leaning#DEFAULT}
 */
public record Personality(
    Map<ResourceLocation, Float> intentMultipliers,
    Map<DayPhase, Map<ResourceLocation, Float>> routine,
    float randomness,
    List<WeightedItem> wildTools,
    List<WeightedItem> likes,
    IdleHabits idle,
    Map<ResourceLocation, Leaning> qualifications
) {
    /** No leanings: every multiplier 1, no randomness, nothing held, nothing wanted, no habits of its own. */
    public static final Personality DEFAULT = new Personality(Map.of(), Map.of(), 0.0F, List.of(), List.of(),
        IdleHabits.DEFAULT, Map.of());

    private static final Codec<Map<ResourceLocation, Float>> MULTIPLIERS_CODEC =
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.floatRange(0.0F, Float.MAX_VALUE));

    public static final Codec<Personality> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        MULTIPLIERS_CODEC.optionalFieldOf("intent_multipliers", Map.of()).forGetter(Personality::intentMultipliers),
        Codec.unboundedMap(DayPhase.CODEC, MULTIPLIERS_CODEC).optionalFieldOf("routine", Map.of()).forGetter(Personality::routine),
        Codec.floatRange(0.0F, 1.0F).optionalFieldOf("randomness", DEFAULT.randomness()).forGetter(Personality::randomness),
        WeightedItem.CODEC.listOf().optionalFieldOf("wild_tools", List.of()).forGetter(Personality::wildTools),
        WeightedItem.CODEC.listOf().optionalFieldOf("likes", List.of()).forGetter(Personality::likes),
        IdleHabits.CODEC.optionalFieldOf("idle", IdleHabits.DEFAULT).forGetter(Personality::idle),
        Codec.unboundedMap(ResourceLocation.CODEC, Leaning.CODEC).optionalFieldOf("qualifications", Map.of())
            .forGetter(Personality::qualifications)
    ).apply(instance, Personality::new));

    public Personality {
        intentMultipliers = Map.copyOf(intentMultipliers);
        routine = routine.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
        wildTools = List.copyOf(wildTools);
        likes = List.copyOf(likes);
        qualifications = Map.copyOf(qualifications);
    }

    /** @return how this kind of pet takes to the exams of the licence with this id */
    public Leaning leaning(ResourceLocation qualification) {
        return qualifications.getOrDefault(qualification, Leaning.DEFAULT);
    }

    /**
     * How a kind of pet takes to one licence's exams.
     *
     * @param aptitude what its odds of passing are multiplied by
     * @param bookBonus what reading the book adds for it, on top of what it adds for anyone
     * @param failReaction the face it pulls on hearing it failed
     */
    public record Leaning(float aptitude, float bookBonus, PetReaction failReaction) {
        /** Passes as the licence's odds say, and is puzzled to fail. */
        public static final Leaning DEFAULT = new Leaning(1.0F, 0.0F, PetReaction.CONFUSED);

        public static final Codec<Leaning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0F, 10.0F).optionalFieldOf("aptitude", DEFAULT.aptitude()).forGetter(Leaning::aptitude),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("book_bonus", DEFAULT.bookBonus()).forGetter(Leaning::bookBonus),
            PetReaction.CODEC.optionalFieldOf("fail_reaction", DEFAULT.failReaction()).forGetter(Leaning::failReaction)
        ).apply(instance, Leaning::new));
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
     *
     * <p>What a pet would buy can depend on what it already has: film only while it has a
     * camera to put it in, and a camera only while it has none. The tools a wild pet spawns
     * holding have no use for either.
     *
     * <p>And what it is for: a camera is the pet's own, never in the pool it picks presents from, and
     * is worth saving for rather than spending the money on little things first.
     *
     * @param requires an item the pet has to be carrying for it to want this at all
     * @param limit how many of this the pet may be carrying and still want one more
     * @param keep whether what it buys of this is always its own, never one of the things it may give its owner
     * @param saveUp whether, while it wants this and cannot yet afford it, it buys nothing else, so
     *               that its money builds up; and buys this as soon as it can
     */
    public record WeightedItem(Item item, Weight weight, Optional<Item> requires, Optional<Integer> limit,
                               boolean keep, boolean saveUp) implements WeightedEntry {
        /** Lazy because the item registry only exists once the game has bootstrapped. */
        public static final Codec<WeightedItem> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(WeightedItem::item),
            ExtraCodecs.POSITIVE_INT.xmap(Weight::of, Weight::asInt).fieldOf("weight").forGetter(WeightedItem::weight),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("requires").forGetter(WeightedItem::requires),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("limit").forGetter(WeightedItem::limit),
            Codec.BOOL.optionalFieldOf("keep", false).forGetter(WeightedItem::keep),
            Codec.BOOL.optionalFieldOf("save_up", false).forGetter(WeightedItem::saveUp)
        ).apply(instance, WeightedItem::new)));

        public WeightedItem(Item item, int weight) {
            this(item, Weight.of(weight), Optional.empty(), Optional.empty(), false, false);
        }

        /** @return the same, wanted only while the pet carries {@code item} */
        public WeightedItem requiring(Item item) {
            return new WeightedItem(this.item, weight, Optional.of(item), limit, keep, saveUp);
        }

        /** @return the same, no longer wanted once the pet carries {@code most} of it */
        public WeightedItem limitedTo(int most) {
            return new WeightedItem(item, weight, requires, Optional.of(most), keep, saveUp);
        }

        /** @return the same, always the pet's own */
        public WeightedItem kept() {
            return new WeightedItem(item, weight, requires, limit, true, saveUp);
        }

        /** @return the same, saved up for */
        public WeightedItem savedUpFor() {
            return new WeightedItem(item, weight, requires, limit, keep, true);
        }

        /**
         * @param carried how many of an item the pet is carrying
         * @return whether the pet wants this now: what it needs to have it has, and it does
         *         not have as many as it wants
         */
        public boolean wantedBy(ToIntFunction<Item> carried) {
            return requires.map(needed -> carried.applyAsInt(needed) > 0).orElse(true)
                && limit.map(most -> carried.applyAsInt(item) < most).orElse(true);
        }

        @Override
        public Weight getWeight() {
            return weight;
        }
    }
}
