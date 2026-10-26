package com.dwinovo.chiikawa.entity.brain.errand.remark;

import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * A thing a pet says in passing about how it is faring, loaded from {@code data/<namespace>/pet_remark/<id>.json} by
 * {@link PetRemarkLoader}: when it does, and how each character goes about it. A pet makes a remark beside whatever it is
 * doing and never stops for it, and the words are the character's own, see {@link VoiceMoment}.
 *
 * <p>A character with no part and no {@code *} to fall back on makes no remark.
 *
 * @param when everything that has to be so for the pet to make it
 * @param cast what each character does, by entity type id; {@code *} is every character without a part of its own
 * @param cooldownTicks how long after making it the pet leaves it alone
 */
public record PetRemark(List<RemarkCue> when, Map<String, Part> cast, int cooldownTicks) {
    /** Whose part it is when it is everybody's. */
    public static final String EVERYONE = "*";

    /**
     * What a character does to make the remark.
     *
     * @param moment the moment of its voice it speaks at; the words are the character's, and a character with none for the
     *               moment says nothing
     * @param gesture a move it makes as it speaks, by the name of its animation, if the model has it
     */
    public record Part(VoiceMoment moment, Optional<String> gesture) {
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VoiceMoment.CODEC.fieldOf("moment").forGetter(Part::moment),
            Codec.STRING.optionalFieldOf("gesture").forGetter(Part::gesture)
        ).apply(instance, Part::new));
    }

    private static final Codec<String> WHO = Codec.STRING.validate(who -> who.equals(EVERYONE) || ResourceLocation.tryParse(who) != null
        ? DataResult.success(who) : DataResult.error(() -> who + " is not a character: an entity type id, or " + EVERYONE));

    public static final Codec<PetRemark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RemarkCue.CODEC.listOf().fieldOf("when").forGetter(PetRemark::when),
        Codec.unboundedMap(WHO, Part.CODEC).fieldOf("cast").forGetter(PetRemark::cast),
        ExtraCodecs.POSITIVE_INT.fieldOf("cooldown_ticks").forGetter(PetRemark::cooldownTicks)
    ).apply(instance, PetRemark::new));

    public PetRemark {
        when = List.copyOf(when);
        cast = Map.copyOf(cast);
    }

    /** @return the part this character plays: its own, else everybody's */
    public Optional<Part> partOf(ResourceLocation character) {
        return Optional.ofNullable(cast.getOrDefault(character.toString(), cast.get(EVERYONE)));
    }

    /** @return whether everything that has to be so for the remark is */
    public boolean holds(RemarkCue.Situation situation) {
        return when.stream().allMatch(cue -> cue.holds(situation));
    }
}
