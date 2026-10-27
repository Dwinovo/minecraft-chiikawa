package com.dwinovo.chiikawa.entity.brain.errand.view;

/**
 * When a pet last had its owner in sight, and when it last saw them come back after being away: what a pet needs to greet an
 * owner who has been gone. Worked out by the pet's sensor; not saved.
 *
 * @param lastSeen the game time the owner was last near
 * @param returnedAt the game time the owner was last seen after an absence, or {@link #NEVER}
 */
public record OwnerSeen(long lastSeen, long returnedAt) {
    /** Not yet. */
    public static final long NEVER = Long.MIN_VALUE;

    /**
     * The sensor looks, and the owner is near.
     *
     * @param before what the pet knew, if it knew anything
     * @param now the game time
     * @param awayTicks how long the owner has to have been out of sight to have been away
     * @return what the pet knows now
     */
    public static OwnerSeen seen(OwnerSeen before, long now, long awayTicks) {
        if (before == null) {
            return new OwnerSeen(now, NEVER);
        }
        return new OwnerSeen(now, now - before.lastSeen >= awayTicks ? now : before.returnedAt);
    }

    /**
     * @param now the game time
     * @param lastingTicks how long a return is worth greeting
     * @return whether the owner came back so lately that the pet is still glad of it
     */
    public boolean returned(long now, long lastingTicks) {
        return returnedAt != NEVER && now - returnedAt <= lastingTicks;
    }
}
