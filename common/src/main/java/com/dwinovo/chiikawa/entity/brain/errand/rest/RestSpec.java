package com.dwinovo.chiikawa.entity.brain.errand.rest;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

/**
 * How long an errand rests, in a data file: a number of ticks, or the name of a number that lives
 * in another file (see {@link RestSources}); and whether the rest is on the errand as a whole, or
 * only on the one thing it was about (the crop it could not reach, the partner it played with).
 *
 * <p>Written as the bare number when it is ticks for the errand, {@code {"ticks": 40, "per": "subject"}}
 * when it is ticks for a subject, and {@code {"from": "chiikawa:camera_retry"}} for a named number.
 *
 * @param amount the ticks, or the name of the source to ask
 * @param per what the rest is on
 */
public record RestSpec(Either<Integer, ResourceLocation> amount, Per per) {
    /** What a rest is on. */
    public enum Per implements StringRepresentable {
        ERRAND("errand"),
        SUBJECT("subject");

        public static final Codec<Per> CODEC = StringRepresentable.fromEnum(Per::values);

        private final String name;

        Per(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** No rest. */
    public static final RestSpec NONE = ticks(0);

    private static final Codec<ResourceLocation> SOURCE = ResourceLocation.CODEC.validate(name ->
        RestSources.isKnown(name) ? DataResult.success(name) : DataResult.error(() -> "Unknown rest source " + name));

    private static final Codec<RestSpec> OBJECT = RecordCodecBuilder.create(instance -> instance.group(
        Codec.mapEither(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks"), SOURCE.fieldOf("from"))
            .forGetter(RestSpec::amount),
        Per.CODEC.optionalFieldOf("per", Per.ERRAND).forGetter(RestSpec::per)
    ).apply(instance, RestSpec::new));

    public static final Codec<RestSpec> CODEC = Codec.either(ExtraCodecs.NON_NEGATIVE_INT, OBJECT).xmap(
        either -> either.map(RestSpec::ticks, spec -> spec),
        spec -> spec.amount.left().isPresent() && spec.per == Per.ERRAND ? Either.left(spec.amount.left().get()) : Either.right(spec));

    /** A rest that must not be nothing: what a gave-up errand rests, or it would be started again in the same breath. */
    public static final Codec<RestSpec> LASTING = CODEC.validate(spec -> spec.lasts()
        ? DataResult.success(spec)
        : DataResult.error(() -> "A rest after giving up must last more than 0 ticks"));

    /** @return a rest of this many ticks on the errand */
    public static RestSpec ticks(int ticks) {
        return new RestSpec(Either.left(ticks), Per.ERRAND);
    }

    /** @return a rest of this many ticks on one subject */
    public static RestSpec ticksPerSubject(int ticks) {
        return new RestSpec(Either.left(ticks), Per.SUBJECT);
    }

    /** @return a rest as long as the named source says, on the errand */
    public static RestSpec from(ResourceLocation source) {
        return new RestSpec(Either.right(source), Per.ERRAND);
    }

    /** @return a rest as long as the named source says, on one subject */
    public static RestSpec fromPerSubject(ResourceLocation source) {
        return new RestSpec(Either.right(source), Per.SUBJECT);
    }

    /**
     * @return whether it rests at all: ticks must be more than none, and a named number is taken to be,
     *         since only the live system it comes from can say
     */
    public boolean lasts() {
        return amount.map(ticks -> ticks > 0, source -> true);
    }
}
