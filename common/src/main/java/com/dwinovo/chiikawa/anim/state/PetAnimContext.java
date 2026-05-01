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
 */
public record PetAnimContext(
        PetMode mode,
        PetJobRole job,
        PetLocomotion locomotion,
        PetAction action,
        PetReaction reaction,
        PetAttention attention
) {
    public PetAnimContext {
        mode = mode == null ? PetMode.FOLLOW : mode;
        job = job == null ? PetJobRole.NONE : job;
        locomotion = locomotion == null ? PetLocomotion.IDLE : locomotion;
        action = action == null ? PetAction.NONE : action;
        reaction = reaction == null ? PetReaction.NONE : reaction;
        attention = attention == null ? PetAttention.NONE : attention;
    }

    public static PetAnimContext base(PetMode mode, int jobId, float walkSpeed) {
        return new PetAnimContext(
                mode,
                PetJobRole.fromId(jobId),
                PetLocomotion.fromWalkSpeed(walkSpeed),
                PetAction.NONE,
                PetReaction.NONE,
                PetAttention.NONE);
    }
}
