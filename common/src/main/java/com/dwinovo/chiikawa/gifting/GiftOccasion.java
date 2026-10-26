package com.dwinovo.chiikawa.gifting;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * What can put a present in a pet's mind: something that happened to it, or something it did for its owner. The
 * pet's mind is told when one happens, see {@code Message.Occasion}; whether it thinks of a present because of it is
 * the chance the gifting settings give it.
 */
public enum GiftOccasion implements StringRepresentable {
    /** It picked up a little thing lying about, a flower. */
    FOUND_SOMETHING,
    /** It was paid for a slip it finished. */
    GOT_PAID,
    /** It heard that it passed an exam. */
    PASSED_EXAM,
    /** Its owner was good to it: fed it something it likes, gave it a meal or some money. */
    WAS_TREATED,
    /** The last shot of a roll of film was taken. */
    FILM_FULL;

    /** The occasion as the gifting settings name it. */
    public static final StringRepresentable.EnumCodec<GiftOccasion> CODEC = StringRepresentable.fromEnum(GiftOccasion::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
