package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.Constants;
import com.dwinovo.chiikawa.entity.brain.errand.rest.RestSpec;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * How an errand goes, loaded from {@code data/<namespace>/pet_errand/<errand>.json} by
 * {@link ErrandSettingsLoader}: one file per errand, named by the errand's own id, so a pack can
 * retune an errand.
 *
 * <p>Server-side only: a player's game is told what a pet is doing, not how it decides.
 *
 * @param priority how much the errand matters: a pet drops what it is doing only for an errand of a
 *                 higher priority, never an equal one, and of the errands it can start only those of
 *                 the highest priority are looked at
 * @param score how much the pet fancies it among errands of its priority, before its personality weighs it
 * @param maxTicks the longest the errand runs before the pet gives up on it
 * @param rests how long the pet leaves the errand alone once it has ended
 * @param yieldsTo errands of the same priority that must have nothing to do before this one is started:
 *                 the farmer's strict order, which no score a personality could close
 * @param overlays things the pet does on the side while on this errand, such as chatting
 * @param startVoice what the pet says on setting off, unless it was on an errand of the same kind
 * @param tuning the errand's own numbers, of a shape of the errand's own
 * @param <T> the shape of the tuning
 */
public record ErrandSettings<T>(int priority, float score, int maxTicks, Rests rests, List<ResourceLocation> yieldsTo,
                                List<ResourceLocation> overlays, Optional<VoiceMoment> startVoice, T tuning) {
    private static final String LOG_PREFIX = "[chiikawa-errand] ";

    /**
     * How long an errand is left alone once it has ended, by how it ended.
     *
     * @param done it did what it set out to do
     * @param gaveUp it could not, or took too long; never nothing, or the pet would set off on it again at once
     * @param interrupted something more important took the pet away
     * @param reports how long after something that is told to the errand later: a photo that was stored, or lost
     */
    public record Rests(RestSpec done, RestSpec gaveUp, RestSpec interrupted, Map<String, RestSpec> reports) {
        public static final Codec<Rests> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RestSpec.CODEC.fieldOf("done").forGetter(Rests::done),
            RestSpec.LASTING.fieldOf("gave_up").forGetter(Rests::gaveUp),
            RestSpec.CODEC.fieldOf("interrupted").forGetter(Rests::interrupted),
            Codec.unboundedMap(Codec.STRING, RestSpec.CODEC).optionalFieldOf("reports", Map.of())
                .forGetter(Rests::reports)
        ).apply(instance, Rests::new));

        /** @return rests with no reports */
        public static Rests of(RestSpec done, RestSpec gaveUp, RestSpec interrupted) {
            return new Rests(done, gaveUp, interrupted, Map.of());
        }
    }

    /**
     * @param tuning the codec of the errand's own numbers
     * @param <T> their shape
     * @return the codec of a whole file
     */
    public static <T> Codec<ErrandSettings<T>> codec(Codec<T> tuning) {
        return RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("priority").forGetter(ErrandSettings::priority),
            Codec.floatRange(0.0F, 1.0F).fieldOf("score").forGetter(ErrandSettings::score),
            ExtraCodecs.POSITIVE_INT.fieldOf("max_ticks").forGetter(ErrandSettings::maxTicks),
            Rests.CODEC.fieldOf("rests").forGetter(ErrandSettings::rests),
            // Always written, though a file without them reads as none: they are lists to change.
            ResourceLocation.CODEC.listOf().optionalFieldOf("yields_to")
                .forGetter(settings -> Optional.of(settings.yieldsTo())),
            ResourceLocation.CODEC.listOf().optionalFieldOf("overlays")
                .forGetter(settings -> Optional.of(settings.overlays())),
            VoiceMoment.CODEC.optionalFieldOf("start_voice").forGetter(ErrandSettings::startVoice),
            tuning.fieldOf("tuning").forGetter(ErrandSettings::tuning)
        ).apply(instance, (priority, score, maxTicks, rests, yieldsTo, overlays, startVoice, tuningValue) ->
            new ErrandSettings<>(priority, score, maxTicks, rests, yieldsTo.orElse(List.of()),
                overlays.orElse(List.of()), startVoice, tuningValue)));
    }

    /**
     * @param errand an errand's id
     * @return the codec of that errand's file, with the tuning of its own shape
     * @throws IllegalArgumentException for an id that is not an errand's
     */
    @SuppressWarnings("unchecked")
    public static Codec<ErrandSettings<?>> codecOf(ResourceLocation errand) {
        ErrandType<?, ?> type = Errands.get(errand);
        if (type == null) {
            throw new IllegalArgumentException("No such errand " + errand);
        }
        return (Codec<ErrandSettings<?>>) (Codec<?>) codec(type.tuningCodec());
    }

    /** @return these settings with the errands that go first, as data names them */
    public ErrandSettings<T> yieldingTo(ResourceLocation... errands) {
        return new ErrandSettings<>(priority, score, maxTicks, rests, List.of(errands), overlays, startVoice, tuning);
    }

    /** @return these settings with the pet saying this on setting off */
    public ErrandSettings<T> startingWith(VoiceMoment voice) {
        return new ErrandSettings<>(priority, score, maxTicks, rests, yieldsTo, overlays, Optional.of(voice), tuning);
    }

    /**
     * What can only be told with the other errands in view: that an errand yields only to other errands that
     * are there and of its own priority, so that "goes first" is a rule of order and not of importance.
     *
     * @param errand this errand's id
     * @param all every errand that decoded, by id
     * @return what is wrong with it, or nothing
     */
    public Optional<String> problemAmong(ResourceLocation errand, Map<ResourceLocation, ErrandSettings<?>> all) {
        for (ResourceLocation other : yieldsTo) {
            ErrandSettings<?> theirs = all.get(other);
            if (other.equals(errand) || theirs == null || theirs.priority != priority) {
                return Optional.of("yields_to " + other + " must name another errand of priority " + priority);
            }
        }
        return Optional.empty();
    }

    private static volatile Map<ResourceLocation, ErrandSettings<?>> byErrand = Collections.emptyMap();
    private static final Set<ResourceLocation> REPORTED = ConcurrentHashMap.newKeySet();

    /**
     * @param errand an errand's id
     * @return how that errand goes, or nothing when no data pack gives it settings: an errand whose
     *         settings are gone is switched off rather than guessed at, and says so once
     */
    public static Optional<ErrandSettings<?>> of(ResourceLocation errand) {
        ErrandSettings<?> settings = byErrand.get(errand);
        if (settings == null && REPORTED.add(errand)) {
            Constants.LOG.error(LOG_PREFIX + errand + " is switched off, it has no settings");
        }
        return Optional.ofNullable(settings);
    }

    /** @return every errand that is loaded, by id */
    public static Map<ResourceLocation, ErrandSettings<?>> all() {
        return byErrand;
    }

    /** Replaces them all, as a data pack reload does. Public so an in-game case can try other settings. */
    public static void replaceAll(Map<ResourceLocation, ErrandSettings<?>> settings) {
        byErrand = Map.copyOf(settings);
        REPORTED.clear();
    }
}
