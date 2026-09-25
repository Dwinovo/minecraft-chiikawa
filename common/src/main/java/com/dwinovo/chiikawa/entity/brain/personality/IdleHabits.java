package com.dwinovo.chiikawa.entity.brain.personality;

import com.dwinovo.chiikawa.utils.ModCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * What a pet does with itself when it has nothing to do, sitting or wandering: every so
 * often it picks one of these by weight — look at a player, look at an animal, stroll
 * off, or just sit there — and keeps at it for as long as the habit says. A pet with a
 * weight of 0 for something never does it; one with nobody in range to look at does
 * something else instead.
 *
 * @param lookAtPlayer looking at a player nearby
 * @param lookAtCreature looking at an animal or another pet nearby
 * @param stroll how much it would rather walk a few steps somewhere (never while sitting)
 * @param rest doing nothing in particular
 */
public record IdleHabits(Glance lookAtPlayer, Glance lookAtCreature, int stroll, Pause rest) {
    /** Longest any habit lasts before the pet picks again, in ticks. */
    public static final int LONGEST_TICKS = 1200;
    /** As far as a pet sees who is around it. */
    public static final float FARTHEST_LOOK = 16.0F;

    // Before DEFAULT: making it loads Glance and Pause, whose codecs use these. A range
    // rather than vanilla's IntProvider, which needs the game's registries to load, and a
    // personality has to be there before them.
    private static final Codec<InclusiveRange<Integer>> TICKS = InclusiveRange.codec(Codec.INT, 1, LONGEST_TICKS);
    private static final Codec<Integer> WEIGHT = Codec.intRange(0, Integer.MAX_VALUE);

    /** A pet with no habits of its own: what every pet did before they had any. */
    public static final IdleHabits DEFAULT = new IdleHabits(
        new Glance(2, 5.0F, new InclusiveRange<>(45, 45)),
        new Glance(2, 5.0F, new InclusiveRange<>(45, 45)),
        2,
        new Pause(1, new InclusiveRange<>(30, 60)));

    public static final Codec<IdleHabits> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ModCodecs.strictOptionalField(Glance.CODEC, "look_at_player", DEFAULT.lookAtPlayer()).forGetter(IdleHabits::lookAtPlayer),
        ModCodecs.strictOptionalField(Glance.CODEC, "look_at_creature", DEFAULT.lookAtCreature()).forGetter(IdleHabits::lookAtCreature),
        ModCodecs.strictOptionalField(WEIGHT, "stroll", DEFAULT.stroll()).forGetter(IdleHabits::stroll),
        ModCodecs.strictOptionalField(Pause.CODEC, "rest", DEFAULT.rest()).forGetter(IdleHabits::rest)
    ).apply(instance, IdleHabits::new));

    /**
     * Looking at someone.
     *
     * @param weight how likely it is to be picked
     * @param range how near, in blocks, someone has to be for the pet to look at them
     * @param ticks how long it keeps looking, unless they go out of range first
     */
    public record Glance(int weight, float range, InclusiveRange<Integer> ticks) {
        public static final Codec<Glance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WEIGHT.fieldOf("weight").forGetter(Glance::weight),
            Codec.floatRange(0.0F, FARTHEST_LOOK).fieldOf("range").forGetter(Glance::range),
            TICKS.fieldOf("ticks").forGetter(Glance::ticks)
        ).apply(instance, Glance::new));

        /** @return how long this look lasts */
        public int sampleTicks(RandomSource random) {
            return sample(ticks, random);
        }
    }

    /**
     * Doing nothing.
     *
     * @param weight how likely it is to be picked
     * @param ticks how long it sits there before picking again
     */
    public record Pause(int weight, InclusiveRange<Integer> ticks) {
        public static final Codec<Pause> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WEIGHT.fieldOf("weight").forGetter(Pause::weight),
            TICKS.fieldOf("ticks").forGetter(Pause::ticks)
        ).apply(instance, Pause::new));

        /** @return how long this pause lasts */
        public int sampleTicks(RandomSource random) {
            return sample(ticks, random);
        }
    }

    private static int sample(InclusiveRange<Integer> ticks, RandomSource random) {
        return Mth.randomBetweenInclusive(random, ticks.minInclusive(), ticks.maxInclusive());
    }
}
