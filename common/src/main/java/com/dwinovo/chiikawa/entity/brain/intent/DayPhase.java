package com.dwinovo.chiikawa.entity.brain.intent;

import com.mojang.serialization.Codec;
import net.minecraft.SharedConstants;
import net.minecraft.util.StringRepresentable;

/**
 * Part of the Minecraft day, from the time of day in ticks (0 is sunrise). Serialized
 * by constant name, as in the {@code routine} of a pet personality.
 */
public enum DayPhase implements StringRepresentable {
    /** 23000 to 3000: dawn and the early morning. */
    MORNING,
    /** 3000 to 12000. */
    DAY,
    /** 12000 to 13000: sunset. */
    EVENING,
    /** 13000 to 23000: dark enough for hostile mobs. */
    NIGHT;

    public static final Codec<DayPhase> CODEC = StringRepresentable.fromEnum(DayPhase::values);

    private static final int DAY_START = 3000;
    private static final int EVENING_START = 12000;
    private static final int NIGHT_START = 13000;
    private static final int MORNING_START = 23000;

    /**
     * @param dayTime time of day in ticks; any value, taken modulo a day
     * @return the phase of the day at that time
     */
    public static DayPhase of(long dayTime) {
        int time = Math.floorMod(dayTime, SharedConstants.TICKS_PER_GAME_DAY);
        if (time >= MORNING_START || time < DAY_START) {
            return MORNING;
        }
        if (time < EVENING_START) {
            return DAY;
        }
        return time < NIGHT_START ? EVENING : NIGHT;
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}
