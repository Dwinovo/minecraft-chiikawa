package com.dwinovo.chiikawa.voice;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * The moments a pet may say something at (design 0.1.1, section 3.2). Each is a place in
 * the game that already reacts to what happened: a pet says its line where it jumps for
 * joy or winces.
 */
public enum VoiceMoment implements StringRepresentable {
    /** Tamed. */
    TAME,
    /** Hurt, and still standing. */
    HURT,
    /** Setting off to fight something. */
    HUNT,
    /** Paid for a finished slip. */
    PAID,
    /** Bought something at a shop. */
    SHOP,
    /** Handed its owner a present. */
    GIFT,
    /** Back from a doll on a cake. */
    REVIVE,
    /** Pottering about with nothing to do; rolled every tick, as vanilla rolls an ambient sound. */
    IDLE;

    public static final Codec<VoiceMoment> CODEC = StringRepresentable.fromEnum(VoiceMoment::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
