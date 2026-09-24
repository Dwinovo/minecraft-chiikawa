package com.dwinovo.chiikawa.social;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/**
 * The {@code SOCIAL_COOLDOWNS} memory: which scenes a pet has lately played with whom, so
 * the same two do not play the same one over and over. Kept by both pets of a pair, so
 * either one starting it again is held back. Immutable, like every memory value; not saved.
 *
 * @param until when each scene with each other pet may be played again, in game time
 */
public record SocialCooldowns(Map<Pair, Long> until) {
    public static final SocialCooldowns NONE = new SocialCooldowns(Map.of());

    public SocialCooldowns {
        until = Map.copyOf(until);
    }

    /**
     * @param interaction the scene
     * @param other the other pet
     * @param gameTime the current game time
     * @return whether this pet has played that scene with that pet too recently
     */
    public boolean coolingDown(Identifier interaction, UUID other, long gameTime) {
        Long end = until.get(new Pair(interaction, other));
        return end != null && gameTime < end;
    }

    /**
     * @param gameTime the current game time
     * @return these cooldowns and one more, those already over left out
     */
    public SocialCooldowns with(Identifier interaction, UUID other, long end, long gameTime) {
        Map<Pair, Long> next = new HashMap<>();
        until.forEach((pair, time) -> {
            if (gameTime < time) {
                next.put(pair, time);
            }
        });
        next.put(new Pair(interaction, other), end);
        return new SocialCooldowns(next);
    }

    /**
     * @param interaction the scene
     * @param other the other pet in it
     */
    public record Pair(Identifier interaction, UUID other) {
    }
}
