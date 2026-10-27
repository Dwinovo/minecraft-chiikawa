package com.dwinovo.chiikawa.entity.brain.sensor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;

/**
 * How far a pet looks round itself and how it tells that its owner is back, loaded from
 * {@code data/<namespace>/pet_surroundings/surroundings.json} by {@link SurroundingsSettingsLoader}: one file for the whole
 * mod, so a pack can replace it outright. Read by {@link PetSurroundingsSensor}.
 *
 * <p>Server-side only. With no file loaded a pet looks at nothing and its pastimes have nothing to go by.
 *
 * @param radius how many blocks round itself a pet sees, for shelter, creatures and things lying about
 * @param awayTicks how long out of sight an owner has to have been to be greeted on coming back
 * @param returnedTicks how long after an owner comes back the pet is still glad of it
 * @param settleTicks how long an owner has to have left off work for it to be over, which is also how long they may leave
 *                    off and be at the same spell of work when they take it up again
 * @param spellTicks how long a spell of work has to have gone on to be worth a word when it is over
 * @param finishedTicks how long after a spell of work is over the pet still thinks of it
 */
public record SurroundingsSettings(int radius, int awayTicks, int returnedTicks, int settleTicks, int spellTicks,
                                   int finishedTicks) {
    public static final Codec<SurroundingsSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.POSITIVE_INT.fieldOf("radius").forGetter(SurroundingsSettings::radius),
        ExtraCodecs.POSITIVE_INT.fieldOf("away_ticks").forGetter(SurroundingsSettings::awayTicks),
        ExtraCodecs.POSITIVE_INT.fieldOf("returned_ticks").forGetter(SurroundingsSettings::returnedTicks),
        ExtraCodecs.POSITIVE_INT.fieldOf("settle_ticks").forGetter(SurroundingsSettings::settleTicks),
        ExtraCodecs.POSITIVE_INT.fieldOf("spell_ticks").forGetter(SurroundingsSettings::spellTicks),
        ExtraCodecs.POSITIVE_INT.fieldOf("finished_ticks").forGetter(SurroundingsSettings::finishedTicks)
    ).apply(instance, SurroundingsSettings::new));

    private static volatile Optional<SurroundingsSettings> current = Optional.empty();

    /** @return how a pet looks round itself, or nothing when no data pack says */
    public static Optional<SurroundingsSettings> current() {
        return current;
    }

    /** Replaces them, as a data pack reload does. Public so an in-game case can try other settings. */
    public static void replace(Optional<SurroundingsSettings> settings) {
        current = settings;
    }
}
