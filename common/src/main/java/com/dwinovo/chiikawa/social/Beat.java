package com.dwinovo.chiikawa.social;

import com.dwinovo.chiikawa.anim.state.PetReaction;
import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.PetGesture;
import com.dwinovo.chiikawa.voice.PetSpeech;
import com.dwinovo.chiikawa.voice.VoiceMoment;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

/**
 * One moment of a pet's part in a scene: a move it makes once, the face it pulls and the
 * line it says, any of them left out. A part has one as it begins, one now and then while
 * it lasts and one as it ends, and each is written the same way — a clap while listening
 * and a sulk at the end of a scene are the same kind of thing.
 *
 * @param animation a move the pet makes once, by name; a pet without that animation just
 *                  does not make it
 * @param reaction the face it pulls
 * @param voice the moment of its lines it says, looked up in its own {@code pet_voice}
 *              lines, so each character says it in its own words or keeps quiet
 */
public record Beat(Optional<String> animation, Optional<PetReaction> reaction, Optional<VoiceMoment> voice) {
    public static final MapCodec<Beat> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.optionalFieldOf("animation").forGetter(Beat::animation),
        PetReaction.CODEC.optionalFieldOf("reaction").forGetter(Beat::reaction),
        VoiceMoment.CODEC.optionalFieldOf("voice").forGetter(Beat::voice)
    ).apply(instance, Beat::new));
    public static final Codec<Beat> CODEC = MAP_CODEC.codec();

    /** The pet makes the move, pulls the face and says the line, all at once. Server side. */
    public void play(AbstractPet pet) {
        animation.ifPresent(name -> PetGesture.make(pet, name));
        reaction.ifPresent(pet::triggerReaction);
        voice.ifPresent(moment -> PetSpeech.say(pet, moment));
    }

    /**
     * A beat played over and over for as long as a part lasts, the first one wait after
     * the part begins.
     *
     * @param everyTicks how long the pet waits before each one, drawn afresh every time
     * @param beat what it does then
     */
    public record Recurring(IntProvider everyTicks, Beat beat) {
        public static final Codec<Recurring> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.POSITIVE_CODEC.fieldOf("every_ticks").forGetter(Recurring::everyTicks),
            MAP_CODEC.forGetter(Recurring::beat)
        ).apply(instance, Recurring::new));

        /** @return how many ticks from now the beat comes round again */
        public int nextWait(RandomSource random) {
            return everyTicks.sample(random);
        }
    }
}
