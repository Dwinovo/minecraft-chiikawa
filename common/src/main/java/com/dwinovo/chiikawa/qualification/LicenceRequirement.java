package com.dwinovo.chiikawa.qualification;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * A grade of a licence or better, named the way the series names grades: {@code min_rank} 3
 * is grade 3, 2 or 1. What a slip asks of the pet that takes it, and what a reward asks of
 * the pet it goes to.
 *
 * @param qualification the licence's id
 * @param minRank the lowest-ranked grade that will do, as the player reads it
 */
public record LicenceRequirement(ResourceLocation qualification, int minRank) {
    public static final MapCodec<LicenceRequirement> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("qualification").forGetter(LicenceRequirement::qualification),
        ExtraCodecs.POSITIVE_INT.fieldOf("min_rank").forGetter(LicenceRequirement::minRank)
    ).apply(instance, LicenceRequirement::new));
    public static final Codec<LicenceRequirement> CODEC = MAP_CODEC.codec();

    /**
     * @return whether a pet standing so with its licences holds the grade or better; nobody
     *         does while no data pack has the licence
     */
    public boolean metBy(PetLicences licences) {
        return Qualifications.get(qualification)
            .map(licence -> {
                int held = licences.get(qualification).held();
                return held > 0 && licence.rank(held) <= minRank;
            })
            .orElse(false);
    }
}
