package com.dwinovo.chiikawa.anim.state;

import com.dwinovo.chiikawa.entity.PetDirective;

/**
 * Snapshot of gameplay state consumed by the animation resolver.
 *
 * @param directive the owner's directive for the pet
 * @param job current job role inferred from the pet's tools
 * @param locomotion coarse movement bucket
 * @param action semantic one-shot action, when known
 * @param reaction short-lived emotional reaction, when known
 * @param attention target category for procedural look-at
 * @param activity code-bounded sustained loop activity (level state); the
 *                 highest-priority candidate when non-{@code NONE} —
 *                 short-circuits the rest of the resolver. Driven by Brain
 *                 behaviors via {@code AbstractPet.setActivity}.
 * @param performance the animation the pet holds while playing its part in a scene with
 *                    another pet, named by data; empty when not in one
 */
public record PetAnimContext(
        PetDirective directive,
        PetJobRole job,
        PetLocomotion locomotion,
        PetAction action,
        PetReaction reaction,
        PetAttention attention,
        PetActivity activity,
        String performance
) {
    public PetAnimContext {
        directive = directive == null ? PetDirective.FOLLOW : directive;
        job = job == null ? PetJobRole.NONE : job;
        locomotion = locomotion == null ? PetLocomotion.IDLE : locomotion;
        action = action == null ? PetAction.NONE : action;
        reaction = reaction == null ? PetReaction.NONE : reaction;
        attention = attention == null ? PetAttention.NONE : attention;
        activity = activity == null ? PetActivity.NONE : activity;
        performance = performance == null ? "" : performance;
    }

    /**
     * Common factory that builds a context from the pet's directive, job id, walk
     * speed, code-bounded activity and the part it plays in a scene. Reaction/action/
     * attention default to {@code NONE} (these are vestigial in the resolver's current logic).
     */
    public static PetAnimContext base(PetDirective directive, int jobId, float walkSpeed, PetActivity activity,
            String performance) {
        return new PetAnimContext(
                directive,
                PetJobRole.fromId(jobId),
                PetLocomotion.fromWalkSpeed(walkSpeed),
                PetAction.NONE,
                PetReaction.NONE,
                PetAttention.NONE,
                activity,
                performance);
    }

    /** Shorthand for a pet not in a scene. */
    public static PetAnimContext base(PetDirective directive, int jobId, float walkSpeed, PetActivity activity) {
        return base(directive, jobId, walkSpeed, activity, "");
    }

    /** Backwards-compatible shorthand defaulting activity to {@link PetActivity#NONE}. */
    public static PetAnimContext base(PetDirective directive, int jobId, float walkSpeed) {
        return base(directive, jobId, walkSpeed, PetActivity.NONE);
    }
}
