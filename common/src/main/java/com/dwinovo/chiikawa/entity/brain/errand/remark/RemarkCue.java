package com.dwinovo.chiikawa.entity.brain.errand.remark;

import com.dwinovo.chiikawa.entity.brain.errand.decide.WhyNot;
import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.Weather;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

/**
 * One thing that has to be so for a pet to make a remark, as the remark's file names it: {@code {"type": "lacked_for",
 * "ticks": 600}}. A remark is made when every cue of its {@code when} holds.
 */
public sealed interface RemarkCue {
    Codec<RemarkCue> CODEC = Kind.CODEC.dispatch("type", RemarkCue::kind, Kind::codec);

    /** @return which kind of cue this is */
    Kind kind();

    /** @return whether it holds for a pet in this situation */
    boolean holds(Situation situation);

    /**
     * What a cue may ask of a pet: nothing it could change.
     *
     * @param whyNot why the pet is not at work, if it is not
     * @param now the current game time
     * @param ownerDistance how far off its owner is, while the owner is a player in the pet's level
     * @param phase the part of the day
     * @param weather what falls on the pet
     */
    record Situation(Optional<WhyNot> whyNot, long now, OptionalDouble ownerDistance, DayPhase phase, Weather weather) {
    }

    /**
     * The pet has been short of something for this long.
     *
     * @param ticks how long
     */
    record LackedFor(int ticks) implements RemarkCue {
        static final MapCodec<LackedFor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks").forGetter(LackedFor::ticks)
        ).apply(instance, LackedFor::new));

        @Override
        public Kind kind() {
            return Kind.LACKED_FOR;
        }

        @Override
        public boolean holds(Situation situation) {
            return situation.whyNot().filter(why -> why.lack().isPresent() && why.lasted(situation.now()) >= ticks).isPresent();
        }
    }

    /**
     * The pet's owner is this near, which is to say there is somebody to hear it.
     *
     * @param blocks how near
     */
    record OwnerWithin(double blocks) implements RemarkCue {
        static final MapCodec<OwnerWithin> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.doubleRange(0.0, 128.0).fieldOf("blocks").forGetter(OwnerWithin::blocks)
        ).apply(instance, OwnerWithin::new));

        @Override
        public Kind kind() {
            return Kind.OWNER_WITHIN;
        }

        @Override
        public boolean holds(Situation situation) {
            return situation.ownerDistance().isPresent() && situation.ownerDistance().getAsDouble() <= blocks;
        }
    }

    /**
     * It is one of these parts of the day.
     *
     * @param in the parts
     */
    record InPhase(Set<DayPhase> in) implements RemarkCue {
        static final MapCodec<InPhase> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DayPhase.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("in").forGetter(InPhase::in)
        ).apply(instance, InPhase::new));

        /** In the order of the day, which is the order they are written in. */
        public InPhase {
            in = in.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(in));
        }

        @Override
        public Kind kind() {
            return Kind.PHASE;
        }

        @Override
        public boolean holds(Situation situation) {
            return in.contains(situation.phase());
        }
    }

    /**
     * It rains on the pet, or it thunders.
     *
     * @param is {@link Weather#RAIN} for any rain, a storm included, or {@link Weather#THUNDER} for a storm only
     */
    record WeatherIs(Weather is) implements RemarkCue {
        static final MapCodec<WeatherIs> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Weather.TO_WAIT_FOR.fieldOf("is").forGetter(WeatherIs::is)
        ).apply(instance, WeatherIs::new));

        @Override
        public Kind kind() {
            return Kind.WEATHER;
        }

        @Override
        public boolean holds(Situation situation) {
            return situation.weather().satisfies(is);
        }
    }

    /** The kinds of cue a file can name, by their {@code type}. */
    enum Kind implements StringRepresentable {
        LACKED_FOR(LackedFor.CODEC),
        OWNER_WITHIN(OwnerWithin.CODEC),
        PHASE(InPhase.CODEC),
        WEATHER(WeatherIs.CODEC);

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final MapCodec<? extends RemarkCue> codec;

        Kind(MapCodec<? extends RemarkCue> codec) {
            this.codec = codec;
        }

        MapCodec<? extends RemarkCue> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
