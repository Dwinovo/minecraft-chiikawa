package com.dwinovo.chiikawa.entity.brain.errand.remark;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@code REMARK_RESTS} memory: which remark a pet has made and until when it leaves it alone, so that a pet short of
 * something does not say so again and again. Immutable, like every memory value; not saved, so a pet that is back from a
 * save may say it once more.
 *
 * @param until when each remark's rest is over, in game time, by the remark's id
 */
public record RemarkRests(Map<ResourceLocation, Long> until) {
    public static final RemarkRests NONE = new RemarkRests(Map.of());

    public RemarkRests {
        until = Map.copyOf(until);
    }

    /** @return whether the pet is leaving this remark alone at this game time */
    public boolean resting(ResourceLocation remark, long now) {
        return until.getOrDefault(remark, 0L) > now;
    }

    /**
     * @param ticks how long from now the rest lasts
     * @param now the current game time
     * @return these rests with this one set, those already over left out
     */
    public RemarkRests with(ResourceLocation remark, long ticks, long now) {
        Map<ResourceLocation, Long> next = new HashMap<>();
        until.forEach((id, end) -> {
            if (now < end) {
                next.put(id, end);
            }
        });
        next.put(remark, now + ticks);
        return new RemarkRests(next);
    }
}
