package com.dwinovo.chiikawa.whistle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * How a whistle behaves, loaded from {@code data/<namespace>/pet_whistle/<item>.json} by
 * {@link WhistleSettingsLoader}: one file per item that can be blown, named by the item's
 * own id, so a pack can retune how the candy blows.
 *
 * <p>Server-side only: a player's game only has to draw the pose and the hold.
 *
 * @param range how far from the owner a pet still hears it, in blocks
 * @param blowTicks how long the item is held before it is blown
 * @param cooldownTicks how long before it can be blown again
 * @param reactDelayTicks the most a pet waits before it answers; each waits its own random
 *                        part of it, so a yard does not move as one
 * @param curiousTicks how long a wild pet that came over to look stays before it goes back
 *                     to what it was doing
 * @param sound what is heard when it is blown
 * @param pitch how high
 */
public record WhistleSettings(double range, int blowTicks, int cooldownTicks, int reactDelayTicks,
                              int curiousTicks, ResourceLocation sound, float pitch) {
    public static final Codec<WhistleSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.doubleRange(1.0, 256.0).fieldOf("range").forGetter(WhistleSettings::range),
        ExtraCodecs.POSITIVE_INT.fieldOf("blow_ticks").forGetter(WhistleSettings::blowTicks),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").forGetter(WhistleSettings::cooldownTicks),
        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("react_delay_ticks").forGetter(WhistleSettings::reactDelayTicks),
        ExtraCodecs.POSITIVE_INT.fieldOf("curious_ticks").forGetter(WhistleSettings::curiousTicks),
        ResourceLocation.CODEC.fieldOf("sound").forGetter(WhistleSettings::sound),
        Codec.floatRange(0.5F, 2.0F).fieldOf("pitch").forGetter(WhistleSettings::pitch)
    ).apply(instance, WhistleSettings::new));

    private static volatile Map<ResourceLocation, WhistleSettings> byItem = Collections.emptyMap();

    /**
     * @param item an item's id
     * @return how that item is blown, or nothing when no data pack makes a whistle of it:
     *         an item whose settings are gone does nothing when used rather than guessing
     */
    public static Optional<WhistleSettings> of(ResourceLocation item) {
        return Optional.ofNullable(byItem.get(item));
    }

    static void replaceAll(Map<ResourceLocation, WhistleSettings> settings) {
        byItem = Map.copyOf(settings);
    }
}
