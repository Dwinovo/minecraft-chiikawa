package com.dwinovo.chiikawa.social;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@code SOCIAL_COOLDOWNS} memory: which scenes a pet has lately played with whom, so
 * the same two do not play the same one over and over. Kept by both pets of a pair, each by its own
 * errand's rest, so either one starting it again is held back. Immutable, like every memory value;
 * not saved.
 *
 * <p>A rest is on a subject, the scene with the other pet (see {@link #subject}); the errand that played
 * the scene is not part of it, since the pet that came over and the pet that was visited rest alike.
 *
 * @param until when each scene with each other pet may be played again, in game time
 */
public record SocialCooldowns(Map<String, Long> until) {
    public static final SocialCooldowns NONE = new SocialCooldowns(Map.of());

    public SocialCooldowns {
        until = Map.copyOf(until);
    }

    /**
     * @param interaction the scene
     * @param other the other pet in it
     * @return what the rest is on: the one name by which the scene's errands rest on it and ask whether they are resting
     */
    public static String subject(ResourceLocation interaction, UUID other) {
        return interaction + "/" + other;
    }

    /**
     * @param subject the scene with another pet, see {@link #subject}
     * @param gameTime the current game time
     * @return how many ticks are left of the rest, 0 for none
     */
    public long remaining(String subject, long gameTime) {
        return Math.max(0L, until.getOrDefault(subject, 0L) - gameTime);
    }

    /**
     * @param interaction the scene
     * @param other the other pet
     * @param gameTime the current game time
     * @return whether this pet has played that scene with that pet too recently
     */
    public boolean coolingDown(ResourceLocation interaction, UUID other, long gameTime) {
        return remaining(subject(interaction, other), gameTime) > 0;
    }

    /**
     * @param subject the scene with another pet, see {@link #subject}
     * @param ticks how long from now it lasts; none or less ends it
     * @param gameTime the current game time
     * @return these cooldowns with this one set, those already over left out
     */
    public SocialCooldowns with(String subject, long ticks, long gameTime) {
        Map<String, Long> next = new HashMap<>();
        until.forEach((key, time) -> {
            if (gameTime < time) {
                next.put(key, time);
            }
        });
        if (ticks > 0) {
            next.put(subject, gameTime + ticks);
        } else {
            next.remove(subject);
        }
        return new SocialCooldowns(next);
    }
}
