package com.dwinovo.chiikawa.entity.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

/**
 * How far and how often a pet of a job looks for the blocks its work is done to, the {@code sight} block of
 * {@code data/<namespace>/pet_job/<job>.json}. Only a job whose work is done to blocks has one, see
 * {@link com.dwinovo.chiikawa.entity.brain.sensor.PetWorkSensor}.
 *
 * @param radius how far from the pet, along each horizontal axis, it looks
 * @param verticalRange how far above and below the pet it looks
 * @param forgetDistance how far from the pet a block it has seen can be before it is forgotten: errands ignore blocks
 *                       outside their anchor's reach, and a kept block stops the pet looking for another
 * @param backoffStepTicks how much longer it waits before looking again for each search in a row that found nothing
 * @param emptyStreakLimit how many searches in a row that found nothing count towards that wait, which caps it, so
 *                         that a pet standing still still notices work that is new
 */
public record Sight(int radius, int verticalRange, double forgetDistance, int backoffStepTicks, int emptyStreakLimit) {
    public static final Codec<Sight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("radius").forGetter(Sight::radius),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("vertical_range").forGetter(Sight::verticalRange),
        Codec.doubleRange(0.0, 256.0).fieldOf("forget_distance").forGetter(Sight::forgetDistance),
        ExtraCodecs.POSITIVE_INT.fieldOf("backoff_step_ticks").forGetter(Sight::backoffStepTicks),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("empty_streak_limit").forGetter(Sight::emptyStreakLimit)
    ).apply(instance, Sight::new));
}
