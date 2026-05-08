package com.dwinovo.chiikawa.anim.state;

import com.dwinovo.chiikawa.entity.PetMode;

import java.util.List;

/**
 * Pure function from gameplay state to an ordered candidate list for the
 * main state-driven controller's base loop.
 *
 * <p>Returns a fallback chain rather than a single name so the controller can
 * pick the first animation that actually exists in the library — animators
 * authoring a new pet can ship just {@code idle} and let the resolver fall
 * back from {@code "run"} → {@code "walk"} → {@code "idle"} until the
 * dedicated cycles arrive.
 *
 * <p>One-shot actions and reactions are <em>not</em> resolved here — they
 * arrive at the renderer as triggered animations on the {@code "action"} /
 * {@code "reaction"} controllers, dispatched from
 * {@link com.dwinovo.chiikawa.entity.AbstractPet}'s synced packets.
 */
public final class PetAnimationResolver {

    private static final List<String> IDLE = List.of("idle");

    private PetAnimationResolver() {
    }

    /** Returns the ordered base-loop candidate list for the pet's current state. */
    public static List<String> resolve(PetAnimContext context) {
        PetAnimContext safe = context == null
                ? PetAnimContext.base(PetMode.FOLLOW, 0, 0f)
                : context;

        // Code-bounded activity (level state) wins over everything else —
        // it represents an explicit "the AI is making the pet do this right
        // now" decision (eat, play guitar, ...). Falls back to "idle" so a
        // pet whose model lacks the specific animation degrades gracefully.
        if (safe.activity() != PetActivity.NONE) {
            String anim = safe.activity().animationName();
            return anim == null ? List.of("idle") : List.of(anim, "idle");
        }

        if (safe.mode() == PetMode.SIT) {
            return List.of("sit", "idle");
        }

        if (safe.locomotion() == PetLocomotion.RUN) {
            return List.of("run", "walk", "idle");
        }
        if (safe.locomotion() == PetLocomotion.WALK) {
            return List.of("walk", "run", "idle");
        }

        if (safe.mode() == PetMode.WORK) {
            return switch (safe.job()) {
                case FARMER -> List.of("work_idle_farmer", "idle");
                case FENCER -> List.of("work_idle_fencer", "idle");
                case ARCHER -> List.of("work_idle_archer", "idle");
                case NONE -> IDLE;
            };
        }

        return IDLE;
    }
}
