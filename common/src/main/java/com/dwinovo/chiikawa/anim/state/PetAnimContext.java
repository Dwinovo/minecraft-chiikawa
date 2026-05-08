package com.dwinovo.chiikawa.anim.state;

import com.dwinovo.chiikawa.entity.PetMode;

/**
 * Snapshot of gameplay state consumed by the animation resolver.
 *
 * @param mode player-selected pet mode
 * @param job current job role inferred from the pet's tools
 * @param locomotion coarse movement bucket
 * @param action semantic one-shot action, when known
 * @param reaction short-lived emotional reaction, when known
 * @param attention target category for procedural look-at
 * @param activity code-bounded sustained loop activity (level state); the
 *                 highest-priority candidate when non-{@code NONE} —
 *                 short-circuits the rest of the resolver. Driven by Brain
 *                 behaviors via {@code AbstractPet.setActivity}.
 */
public record PetAnimContext(
        PetMode mode,
        PetJobRole job,
        PetLocomotion locomotion,
        PetAction action,
        PetReaction reaction,
        PetAttention attention,
        PetActivity activity
) {
    public PetAnimContext {
        mode = mode == null ? PetMode.FOLLOW : mode;
        job = job == null ? PetJobRole.NONE : job;
        locomotion = locomotion == null ? PetLocomotion.IDLE : locomotion;
        action = action == null ? PetAction.NONE : action;
        reaction = reaction == null ? PetReaction.NONE : reaction;
        attention = attention == null ? PetAttention.NONE : attention;
        activity = activity == null ? PetActivity.NONE : activity;
    }

    /**
     * Common factory that builds a context from the pet's mode, job id, walk
     * speed, and code-bounded activity. Reaction/action/attention default to
     * {@code NONE} (these are vestigial in the resolver's current logic).
     */
    public static PetAnimContext base(PetMode mode, int jobId, float walkSpeed, PetActivity activity) {
        return new PetAnimContext(
                mode,
                PetJobRole.fromId(jobId),
                PetLocomotion.fromWalkSpeed(walkSpeed),
                PetAction.NONE,
                PetReaction.NONE,
                PetAttention.NONE,
                activity);
    }

    /** Backwards-compatible shorthand defaulting activity to {@link PetActivity#NONE}. */
    public static PetAnimContext base(PetMode mode, int jobId, float walkSpeed) {
        return base(mode, jobId, walkSpeed, PetActivity.NONE);
    }
}
