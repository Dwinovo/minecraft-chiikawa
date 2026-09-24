package com.dwinovo.chiikawa.voice;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;

/**
 * What one kind of pet says, and when: its lines for each moment, how likely it is to
 * speak up then, and how long it keeps quiet afterwards. Loaded per entity type from
 * {@code data/<namespace>/pet_voice/<entity>.json} by {@link PetVoiceLoader}.
 *
 * <p>A moment with no lines is one this pet lets pass in silence: Rakko does not cry out
 * when it is hurt, so it has nothing to say then rather than a line nobody would believe.
 *
 * @param lines what the pet may say at each moment, by weight; each line a translation key
 * @param chance how likely the pet is to say anything at a moment, from 0 to 1; moments not
 *               listed are 1. For {@link VoiceMoment#IDLE} it is the chance each tick
 * @param cooldownTicks how long after saying something the pet stays quiet, whatever happens
 * @param crowdLimit how many pets within earshot may already be talking for this one to
 *                   speak as well; a yard full of pets takes turns rather than all talking
 *                   over each other
 * @param talkTicks how long a line stays up over the pet's head
 * @param hearingRange how far away, in blocks, a player still hears the pet, and other pets
 *                     count as talking over it
 */
public record PetVoice(
    Map<VoiceMoment, List<Line>> lines,
    Map<VoiceMoment, Float> chance,
    int cooldownTicks,
    int crowdLimit,
    int talkTicks,
    double hearingRange
) {
    /** Nothing to say: what a pet with no voice file goes by. */
    public static final PetVoice SILENT = new PetVoice(Map.of(), Map.of(), 0, 1, 1, 0.0);

    public static final Codec<PetVoice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(VoiceMoment.CODEC, Line.CODEC.listOf()).fieldOf("lines").forGetter(PetVoice::lines),
        Codec.unboundedMap(VoiceMoment.CODEC, Codec.floatRange(0.0F, 1.0F))
            .optionalFieldOf("chance", Map.of()).forGetter(PetVoice::chance),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").forGetter(PetVoice::cooldownTicks),
        ExtraCodecs.POSITIVE_INT.fieldOf("crowd_limit").forGetter(PetVoice::crowdLimit),
        ExtraCodecs.POSITIVE_INT.fieldOf("talk_ticks").forGetter(PetVoice::talkTicks),
        Codec.doubleRange(0.0, 128.0).fieldOf("hearing_range").forGetter(PetVoice::hearingRange)
    ).apply(instance, PetVoice::new));

    public PetVoice {
        lines = lines.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        chance = Map.copyOf(chance);
    }

    /**
     * Whether the pet speaks up at this moment and, if it does, which of its lines it says.
     *
     * @param random the pet's random source
     * @return the line's translation key, or nothing when the pet keeps quiet
     */
    public Optional<String> draw(VoiceMoment moment, RandomSource random) {
        List<Line> said = lines.getOrDefault(moment, List.of());
        if (said.isEmpty()) {
            return Optional.empty();
        }
        float likely = chance.getOrDefault(moment, 1.0F);
        if (likely < 1.0F && random.nextFloat() >= likely) {
            return Optional.empty();
        }
        return WeightedRandom.getRandomItem(random, said).map(Line::text);
    }

    /**
     * One thing a pet may say, and how often it says this rather than its other lines.
     *
     * @param text the translation key of the words
     */
    public record Line(String text, Weight weight) implements WeightedEntry {
        public static final Codec<Line> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("text").forGetter(Line::text),
            ExtraCodecs.POSITIVE_INT.xmap(Weight::of, Weight::asInt).fieldOf("weight").forGetter(Line::weight)
        ).apply(instance, Line::new));

        public Line(String text, int weight) {
            this(text, Weight.of(weight));
        }

        @Override
        public Weight getWeight() {
            return weight;
        }
    }
}
