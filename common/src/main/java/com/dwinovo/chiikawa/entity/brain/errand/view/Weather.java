package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * What falls where a pet is: the weather of its level, as the biome it stands in lets it fall. A level without weather, and a
 * biome where it snows or never rains, have {@link #CLEAR}.
 */
public enum Weather implements StringRepresentable {
    CLEAR,
    RAIN,
    THUNDER;

    public static final Codec<Weather> CODEC = StringRepresentable.fromEnum(Weather::values);

    /**
     * @param raining whether it rains in the level
     * @param thundering whether it thunders in the level
     * @param rainsHere whether the biome the pet is in lets the rain fall as rain, and not as snow or not at all
     * @return what falls on the pet
     */
    public static Weather of(boolean raining, boolean thundering, boolean rainsHere) {
        if (!raining || !rainsHere) {
            return CLEAR;
        }
        return thundering ? THUNDER : RAIN;
    }

    /** @return whether it rains on the pet, in a storm as in a shower */
    public boolean raining() {
        return this != CLEAR;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
