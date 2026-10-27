package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
    /** The weather a cue waits for. Clear weather is not weather to wait for: the cue would hold when no cue was named. */
    public static final Codec<Weather> TO_WAIT_FOR = CODEC.validate(weather -> weather == CLEAR
        ? DataResult.error(() -> "weather to wait for is rain or thunder") : DataResult.success(weather));

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

    /**
     * @param wanted the weather a cue waits for
     * @return whether this is it: {@link #RAIN} for any rain, a storm included, or {@link #THUNDER} for a storm only
     */
    public boolean satisfies(Weather wanted) {
        return wanted == RAIN ? raining() : this == wanted;
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
