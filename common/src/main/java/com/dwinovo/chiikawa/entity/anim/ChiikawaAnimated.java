package com.dwinovo.chiikawa.entity.anim;

import com.dwinovo.chiikawa.anim.runtime.Animator;

/**
 * Marker interface for entities driven by the chiikawa animation pipeline.
 * Implementing this lets the entity renderer find the per-entity
 * {@link Animator} and pull a {@link PetAnimContext} for the state-driven
 * controllers.
 *
 * <p>All pet entities implement this through {@code AbstractPet}.
 */
public interface ChiikawaAnimated {

    /** Per-entity animator. Holds one {@code ControllerInstance} per controller of the renderer's roster. */
    Animator<PetAnimContext> getPetAnimator();

    /**
     * Snapshot of gameplay state, consumed by every controller's state
     * handler. Built per extract from live entity data.
     *
     * @param walkSpeed normalised walk speed sampled from the entity, used
     *                  by the resolver to bucket idle / walk / run
     */
    PetAnimContext getAnimContext(float walkSpeed);
}
