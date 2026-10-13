package com.dwinovo.chiikawa.entity.brain.constraint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;

/**
 * How big the area a pet acts in is, in blocks, loaded from
 * {@code data/<namespace>/pet_anchor/anchor.json} by {@link AnchorSettingsLoader}: one file for the
 * whole mod, so a pack can replace it outright. Where the area is, and what it is the middle of, is
 * {@link PetConstraints}'s; this is only its size.
 *
 * <p>Server-side only. With no file loaded no pet has an area to act in: every pet stays where it
 * is, which loses no pet and shows at once that the settings are missing.
 *
 * @param follow a pet at its owner's heel
 * @param free a pet told to roam, round the home it was left at
 * @param wild a pet nobody owns, round where it spawned
 */
public record AnchorSettings(Follow follow, Area free, Area wild) {
    /**
     * @param start how far from its owner a following pet starts walking back
     * @param arrive how near, in blocks counted along the axes, a pet walking to its owner has arrived
     * @param reach how far from its owner an errand may start
     * @param leash how far from its owner an errand may go on, past which the pet is walked back
     * @param teleport how far behind its owner a following pet is fetched
     */
    public record Follow(double start, int arrive, double reach, double leash, double teleport) {
        /** What a pet that follows nobody reads; no anchor follows when there are no settings, so it is never used. */
        public static final Follow NONE = new Follow(0.0, 0, 0.0, 0.0, 0.0);

        public static final Codec<Follow> CODEC = ExtraCodecs.validate(RecordCodecBuilder.<Follow>create(instance -> instance.group(
            Codec.doubleRange(0.0, 256.0).fieldOf("start").forGetter(Follow::start),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("arrive").forGetter(Follow::arrive),
            Codec.doubleRange(0.0, 256.0).fieldOf("reach").forGetter(Follow::reach),
            Codec.doubleRange(0.0, 256.0).fieldOf("leash").forGetter(Follow::leash),
            Codec.doubleRange(0.0, 256.0).fieldOf("teleport").forGetter(Follow::teleport)
        ).apply(instance, Follow::new)), follow -> follow.start <= follow.reach
                && follow.reach <= follow.leash && follow.leash <= follow.teleport
            ? DataResult.success(follow)
            : DataResult.error(() -> "a following pet's distances run start, reach, leash, teleport, from nearest to furthest"));
    }

    /**
     * @param reach how far from the middle an errand may start
     * @param leash how far it may go on, past which the pet is walked back
     */
    public record Area(double reach, double leash) {
        public static final Codec<Area> CODEC = ExtraCodecs.validate(RecordCodecBuilder.<Area>create(instance -> instance.group(
            Codec.doubleRange(0.0, 256.0).fieldOf("reach").forGetter(Area::reach),
            Codec.doubleRange(0.0, 256.0).fieldOf("leash").forGetter(Area::leash)
        ).apply(instance, Area::new)), area -> area.reach <= area.leash
            ? DataResult.success(area)
            : DataResult.error(() -> "the leash is no shorter than the reach"));
    }

    public static final Codec<AnchorSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Follow.CODEC.fieldOf("follow").forGetter(AnchorSettings::follow),
        Area.CODEC.fieldOf("free").forGetter(AnchorSettings::free),
        Area.CODEC.fieldOf("wild").forGetter(AnchorSettings::wild)
    ).apply(instance, AnchorSettings::new));

    private static volatile Optional<AnchorSettings> current = Optional.empty();

    /** @return the sizes of the areas pets act in, or nothing when no data pack says */
    public static Optional<AnchorSettings> current() {
        return current;
    }

    static void replace(Optional<AnchorSettings> settings) {
        current = settings;
    }
}
