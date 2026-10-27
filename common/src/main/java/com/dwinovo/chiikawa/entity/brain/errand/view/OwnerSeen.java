package com.dwinovo.chiikawa.entity.brain.errand.view;

/**
 * When a pet last had its owner in sight, when it last saw them come back after being away, and the spell of work it last
 * saw them at: what a pet needs to greet an owner who has been gone, and to see that one has finished a job of work. Worked
 * out by the pet's sensor; not saved.
 *
 * @param lastSeen the game time the owner was last near
 * @param returnedAt the game time the owner was last seen after an absence, or {@link #NEVER}
 * @param workedFrom the game time the spell of work last seen began, or {@link #NEVER}
 * @param workedAt the game time the owner was last seen at work, or {@link #NEVER}
 */
public record OwnerSeen(long lastSeen, long returnedAt, long workedFrom, long workedAt) {
    /** Not yet. */
    public static final long NEVER = Long.MIN_VALUE;

    /**
     * The sensor looks, and the owner is near.
     *
     * @param before what the pet knew, if it knew anything
     * @param now the game time
     * @param awayTicks how long out of sight the owner has to have been to have been away
     * @param working whether the owner is at work now
     * @param settleTicks how long the owner may leave off and be at the same spell of work when they take it up again
     * @return what the pet knows now
     */
    public static OwnerSeen seen(OwnerSeen before, long now, long awayTicks, boolean working, long settleTicks) {
        if (before == null) {
            return new OwnerSeen(now, NEVER, working ? now : NEVER, working ? now : NEVER);
        }
        long from = before.workedFrom;
        long at = before.workedAt;
        if (working) {
            from = at != NEVER && now - at <= settleTicks ? from : now;
            at = now;
        }
        return new OwnerSeen(now, now - before.lastSeen >= awayTicks ? now : before.returnedAt, from, at);
    }

    /**
     * @param now the game time
     * @param lastingTicks how long a return is worth greeting
     * @return whether the owner came back so lately that the pet is still glad of it
     */
    public boolean returned(long now, long lastingTicks) {
        return returnedAt != NEVER && now - returnedAt <= lastingTicks;
    }

    /**
     * @param now the game time
     * @param settleTicks how long the owner has to have left off for the work to be over
     * @param spellTicks how long a spell of work has to have gone on to be worth a word
     * @param lastingTicks how long after it is over it is still worth one
     * @return whether the owner has lately left off a long spell of work
     */
    public boolean finishedWork(long now, long settleTicks, long spellTicks, long lastingTicks) {
        long idle = now - workedAt;
        return workedAt != NEVER && idle >= settleTicks && idle <= settleTicks + lastingTicks && workedAt - workedFrom >= spellTicks;
    }
}
