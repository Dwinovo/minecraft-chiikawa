package com.dwinovo.chiikawa.settings;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/**
 * The few things a world's players can turn up or down without writing a data pack. They
 * are multipliers on what the data says, never a second copy of it: the pack still decides
 * who spawns where and what a pet may say, and these only scale how much of that happens.
 *
 * <p>Whatever is handed in is brought into range, so a value that arrives off the wire or
 * out of a file can never be one the game has no meaning for.
 *
 * @param wildSpawn how much of the wild spawning that would happen does, from 0 (none) to 1
 *                  (all of it). Only ever down: more spawns means a bigger weight in the pack
 * @param chatter how much more or less a pet speaks up than its voice data says, from 0
 *                (silent) to {@link #MAX_CHATTER}
 */
public record WorldSettings(float wildSpawn, float chatter) {
    /** The most the chatter may be turned up: twice as often as the data says. */
    public static final float MAX_CHATTER = 2.0F;

    /** How a world is until somebody changes it: nothing scaled. */
    public static final WorldSettings DEFAULT = new WorldSettings(1.0F, 1.0F);

    public static WorldSettings read(FriendlyByteBuf buffer) {
        return new WorldSettings(buffer.readFloat(), buffer.readFloat());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeFloat(wildSpawn);
        buffer.writeFloat(chatter);
    }

    public WorldSettings {
        wildSpawn = within(wildSpawn, 1.0F, 1.0F);
        chatter = within(chatter, MAX_CHATTER, 1.0F);
    }

    /** In range, or {@code fallback} for a number that is not one. */
    private static float within(float value, float max, float fallback) {
        return Float.isNaN(value) ? fallback : Mth.clamp(value, 0.0F, max);
    }
}
