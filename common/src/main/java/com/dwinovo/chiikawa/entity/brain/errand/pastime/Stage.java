package com.dwinovo.chiikawa.entity.brain.errand.pastime;

import com.dwinovo.chiikawa.social.Beat;
import com.dwinovo.chiikawa.social.PartPlayer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;

/**
 * What one character does to pass the time with a pastime: where it goes first, if it goes anywhere, and then how it
 * carries on for a while, written as a pet's part in a scene with another pet is, see {@code PetInteraction.Side}: a pose it
 * holds throughout, and a beat as it begins, one now and then while it lasts and one as it ends.
 *
 * @param to where it goes before it begins, for a pastime that goes somewhere; the pet's own file says whom it is for
 * @param pose what it holds for as long as it lasts, by the name of its animation, several separated by commas for the
 *             first the character has; a character without any simply stands there
 * @param begin what it does as it begins
 * @param nowAndThen what it does over and over while it lasts
 * @param end what it does as it is over, however it ends
 * @param ticks how long it lasts
 */
public record Stage(Optional<Destination> to, Optional<String> pose, Optional<Beat> begin, Optional<Beat.Recurring> nowAndThen,
                    Optional<Beat> end, int ticks) implements PartPlayer.Script {
    public static final Codec<Stage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Destination.CODEC.optionalFieldOf("to").forGetter(Stage::to),
        Codec.STRING.optionalFieldOf("pose").forGetter(Stage::pose),
        Beat.CODEC.optionalFieldOf("begin").forGetter(Stage::begin),
        Beat.Recurring.CODEC.optionalFieldOf("now_and_then").forGetter(Stage::nowAndThen),
        Beat.CODEC.optionalFieldOf("end").forGetter(Stage::end),
        ExtraCodecs.POSITIVE_INT.fieldOf("ticks").forGetter(Stage::ticks)
    ).apply(instance, Stage::new));
}
