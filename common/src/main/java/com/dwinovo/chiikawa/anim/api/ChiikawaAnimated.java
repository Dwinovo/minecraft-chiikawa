package com.dwinovo.chiikawa.anim.api;

import com.dwinovo.chiikawa.anim.runtime.PetAnimator;
import com.dwinovo.chiikawa.anim.state.PetAnimContext;
import com.dwinovo.chiikawa.anim.state.PetAnimationResolver;

/**
 * Marker interface for entities driven by the Bedrock animation pipeline.
 * Implementing this lets the entity renderer find the per-entity
 * {@link PetAnimator} directly.
 *
 * <p>All pet entities implement this through {@code AbstractPet}.
 */
public interface ChiikawaAnimated {

    PetAnimator getPetAnimator();

    /**
     * Returns the gameplay snapshot that the animation resolver uses to pick
     * channel animations. The renderer prefixes resolver output with the model
     * key to find {@link AnimationLibrary} entries.
     *
     * @param walkSpeed normalized walk speed sampled from the entity, used to
     *                  distinguish stationary vs moving states
     * @return gameplay state snapshot for animation resolution
     */
    PetAnimContext getAnimContext(float walkSpeed);

    /**
     * Legacy helper for callers that still want a single base-loop name.
     *
     * @param walkSpeed normalized walk speed sampled from the entity
     * @return first base-loop candidate selected by the resolver
     */
    @Deprecated(forRemoval = false)
    default String getMainAnimationName(float walkSpeed) {
        return PetAnimationResolver.resolve(getAnimContext(walkSpeed))
                .baseLoopCandidates()
                .stream()
                .findFirst()
                .orElse("idle");
    }
}
