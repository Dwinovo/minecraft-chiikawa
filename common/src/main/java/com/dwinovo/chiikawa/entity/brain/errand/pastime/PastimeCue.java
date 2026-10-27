package com.dwinovo.chiikawa.entity.brain.errand.pastime;

import com.dwinovo.chiikawa.entity.brain.errand.view.DayPhase;
import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import com.dwinovo.chiikawa.entity.brain.errand.view.Surroundings;
import com.dwinovo.chiikawa.entity.brain.errand.view.Weather;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * One thing that has to be so for a pet to pass the time with a pastime, as the pastime's file names it:
 * {@code {"type": "weather", "is": "rain"}}. A pastime is done only while every cue of its {@code when} holds. A cue reads
 * the pet only through its {@link PetView}, as an errand's offer does, so it is as pure as one: it answers the same when
 * asked twice, and changes nothing.
 */
public sealed interface PastimeCue {
    Codec<PastimeCue> CODEC = Kind.CODEC.dispatch("type", PastimeCue::kind, Kind::codec);

    /** @return which kind of cue this is */
    Kind kind();

    /** @return whether it holds for this pet now */
    boolean holds(PetView view);

    /**
     * It rains on the pet, or it thunders.
     *
     * @param is {@link Weather#RAIN} for any rain, a storm included, or {@link Weather#THUNDER} for a storm only
     */
    record WeatherIs(Weather is) implements PastimeCue {
        static final MapCodec<WeatherIs> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Weather.CODEC.flatXmap(WeatherIs::fallsOnAPet, WeatherIs::fallsOnAPet).fieldOf("is").forGetter(WeatherIs::is)
        ).apply(instance, WeatherIs::new));

        /** Clear weather is not weather to wait for: the cue would hold when no cue was named. */
        private static DataResult<Weather> fallsOnAPet(Weather weather) {
            return weather == Weather.CLEAR ? DataResult.error(() -> "weather to wait for is rain or thunder") : DataResult.success(weather);
        }

        @Override
        public Kind kind() {
            return Kind.WEATHER;
        }

        @Override
        public boolean holds(PetView view) {
            return is == Weather.RAIN ? view.weather().raining() : view.weather() == is;
        }
    }

    /** The pet is out in the open, where the rain falls on it. */
    record UnderSky() implements PastimeCue {
        static final MapCodec<UnderSky> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.point(new UnderSky()));

        @Override
        public Kind kind() {
            return Kind.UNDER_SKY;
        }

        @Override
        public boolean holds(PetView view) {
            return view.underSky();
        }
    }

    /**
     * It is one of these parts of the day.
     *
     * @param in the parts
     */
    record InPhase(Set<DayPhase> in) implements PastimeCue {
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
        public boolean holds(PetView view) {
            return in.contains(view.phase());
        }
    }

    /** What frightens a pet. */
    enum Fright implements StringRepresentable {
        /** A storm. */
        THUNDER,
        /** A creeper about to go off. */
        CREEPER,
        /** Something that has just hurt it. */
        HURT;

        static final Codec<Fright> CODEC = StringRepresentable.fromEnum(Fright::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The pet has a reason to be frightened.
     *
     * @param by what of
     */
    record ScaredBy(Fright by) implements PastimeCue {
        static final MapCodec<ScaredBy> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Fright.CODEC.fieldOf("by").forGetter(ScaredBy::by)
        ).apply(instance, ScaredBy::new));

        @Override
        public Kind kind() {
            return Kind.SCARED;
        }

        @Override
        public boolean holds(PetView view) {
            return switch (by) {
                case THUNDER -> view.weather() == Weather.THUNDER;
                case CREEPER -> view.surroundings().primedCreeper();
                case HURT -> view.attacker().isPresent();
            };
        }
    }

    /** What the owner is up to. */
    enum OwnerState implements StringRepresentable {
        /** Only there. */
        NEAR,
        /** At work with a tool or a block in hand. */
        WORKING,
        /** Has just come back after a while out of sight. */
        RETURNED,
        /** Asleep in a bed. */
        SLEEPING;

        static final Codec<OwnerState> CODEC = StringRepresentable.fromEnum(OwnerState::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The owner is near the pet and doing this.
     *
     * @param is what they are up to
     * @param range how near, in blocks
     */
    record OwnerIs(OwnerState is, double range) implements PastimeCue {
        static final MapCodec<OwnerIs> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            OwnerState.CODEC.fieldOf("is").forGetter(OwnerIs::is),
            Codec.doubleRange(0.0, 128.0).fieldOf("range").forGetter(OwnerIs::range)
        ).apply(instance, OwnerIs::new));

        @Override
        public Kind kind() {
            return Kind.OWNER;
        }

        @Override
        public boolean holds(PetView view) {
            boolean near = view.ownerPos().filter(owner -> owner.dimension().equals(view.pos().dimension())
                && owner.pos().distSqr(view.pos().pos()) <= range * range).isPresent();
            return near && switch (is) {
                case NEAR -> true;
                case WORKING -> view.ownerWorking();
                case RETURNED -> view.ownerReturned();
                case SLEEPING -> view.ownerBedside().isPresent();
            };
        }
    }

    /**
     * The pet sees a creature of a kind, or a thing lying about, this near.
     *
     * @param what the kind of creature, or of item, by the tag that names them
     * @param range how near, in blocks
     */
    record Sees(Either<TagKey<EntityType<?>>, TagKey<Item>> what, double range) implements PastimeCue {
        static final MapCodec<Sees> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.mapEither(TagKey.hashedCodec(Registries.ENTITY_TYPE).fieldOf("entities"),
                TagKey.hashedCodec(Registries.ITEM).fieldOf("items")).forGetter(Sees::what),
            Codec.doubleRange(0.0, 128.0).fieldOf("range").forGetter(Sees::range)
        ).apply(instance, Sees::new));

        @Override
        public Kind kind() {
            return Kind.SEES;
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean holds(PetView view) {
            Surroundings around = view.surroundings();
            BlockPos here = view.pos().pos();
            return what.map(
                creatures -> around.creatures().stream().anyMatch(seen -> seen.kind().is(creatures) && near(here, seen.where())),
                items -> around.items().stream().anyMatch(seen -> seen.kind().builtInRegistryHolder().is(items) && near(here, seen.where())));
        }

        private boolean near(BlockPos here, BlockPos there) {
            return here.distSqr(there) <= range * range;
        }
    }

    /** The pet is short of something its work needs. */
    record Lacking() implements PastimeCue {
        static final MapCodec<Lacking> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.point(new Lacking()));

        @Override
        public Kind kind() {
            return Kind.LACKING;
        }

        @Override
        public boolean holds(PetView view) {
            return view.lack().isPresent();
        }
    }

    /** The kinds of cue a file can name, by their {@code type}. */
    enum Kind implements StringRepresentable {
        WEATHER(WeatherIs.CODEC),
        UNDER_SKY(UnderSky.CODEC),
        PHASE(InPhase.CODEC),
        SCARED(ScaredBy.CODEC),
        OWNER(OwnerIs.CODEC),
        SEES(Sees.CODEC),
        LACKING(Lacking.CODEC);

        static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final MapCodec<? extends PastimeCue> codec;

        Kind(MapCodec<? extends PastimeCue> codec) {
            this.codec = codec;
        }

        MapCodec<? extends PastimeCue> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
