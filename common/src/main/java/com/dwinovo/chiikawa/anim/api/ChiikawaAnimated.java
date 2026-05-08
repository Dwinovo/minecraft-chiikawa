package com.dwinovo.chiikawa.anim.api;

import com.dwinovo.chiikawa.anim.runtime.PetAnimator;
import com.dwinovo.chiikawa.anim.state.PetAnimContext;

/**
 * Marker interface for entities driven by the chiikawa animation pipeline.
 * Implementing this lets the entity renderer find the per-entity
 * {@link PetAnimator} and pull a {@link PetAnimContext} for the state-driven
 * controllers.
 *
 * <p>All pet entities implement this through {@code AbstractPet}.
 */
public interface ChiikawaAnimated {

    /** Per-entity animator. Holds one {@code ControllerInstance} per registered controller. */
    PetAnimator getPetAnimator();

    /**
     * Snapshot of gameplay state, consumed by every controller's state
     * handler. Built per extract from live entity data.
     *
     * @param walkSpeed normalised walk speed sampled from the entity, used
     *                  by the resolver to bucket idle / walk / run
     */
    PetAnimContext getAnimContext(float walkSpeed);
}
