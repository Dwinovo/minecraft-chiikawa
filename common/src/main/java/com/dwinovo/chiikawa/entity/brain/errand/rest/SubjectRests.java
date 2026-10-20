package com.dwinovo.chiikawa.entity.brain.errand.rest;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@code SUBJECT_RESTS} memory: which crop a farmer could not reach, which item it could not get
 * to, and until when it leaves them alone, so a pet is not sent back at once to what it just gave up
 * on. Immutable, like every memory value; not saved.
 *
 * @param until when each errand's rest on each subject is over, in game time
 */
public record SubjectRests(Map<Key, Long> until) {
    public static final SubjectRests NONE = new SubjectRests(Map.of());

    public SubjectRests {
        until = Map.copyOf(until);
    }

    /**
     * @param errand the errand
     * @param subject what it was about
     * @param now the current game time
     * @return how many ticks are left of the rest, 0 for none
     */
    public long remaining(ResourceLocation errand, String subject, long now) {
        return Math.max(0L, until.getOrDefault(new Key(errand, subject), 0L) - now);
    }

    /**
     * @param ticks how long from now the rest lasts; none or less ends it
     * @param now the current game time
     * @return these rests with this one set, those already over left out
     */
    public SubjectRests with(ResourceLocation errand, String subject, long ticks, long now) {
        Map<Key, Long> next = new HashMap<>();
        until.forEach((key, end) -> {
            if (now < end) {
                next.put(key, end);
            }
        });
        if (ticks > 0) {
            next.put(new Key(errand, subject), now + ticks);
        } else {
            next.remove(new Key(errand, subject));
        }
        return new SubjectRests(next);
    }

    /**
     * @param errand the errand
     * @param subject what it was about
     */
    public record Key(ResourceLocation errand, String subject) {
    }
}
