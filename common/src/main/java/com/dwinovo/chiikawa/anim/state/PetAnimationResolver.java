package com.dwinovo.chiikawa.anim.state;

import com.dwinovo.chiikawa.entity.PetMode;

import java.util.List;

/** Pure mapping from gameplay state to animation channel candidates. */
public final class PetAnimationResolver {
    private static final List<String> IDLE = List.of("idle");

    private PetAnimationResolver() {
    }

    public static PetAnimPlan resolve(PetAnimContext context) {
        PetAnimContext safe = context == null
                ? PetAnimContext.base(PetMode.FOLLOW, 0, 0f)
                : context;
        return new PetAnimPlan(
                resolveBaseLoop(safe),
                safe.action().animationCandidates(),
                List.of(),
                safe.reaction().animationCandidates());
    }

    private static List<String> resolveBaseLoop(PetAnimContext context) {
        if (context.mode() == PetMode.SIT) {
            return List.of("sit", "idle");
        }

        if (context.locomotion() == PetLocomotion.RUN) {
            return List.of("run", "walk", "idle");
        }
        if (context.locomotion() == PetLocomotion.WALK) {
            return List.of("walk", "run", "idle");
        }

        if (context.mode() == PetMode.WORK) {
            return switch (context.job()) {
                case FARMER -> List.of("work_idle_farmer", "idle");
                case FENCER -> List.of("work_idle_fencer", "idle");
                case ARCHER -> List.of("work_idle_archer", "idle");
                case NONE -> IDLE;
            };
        }

        return IDLE;
    }
}
