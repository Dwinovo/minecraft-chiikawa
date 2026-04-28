package com.dwinovo.chiikawa.anim.api;

import com.dwinovo.chiikawa.anim.runtime.PetAnimator;

/**
 * Marker interface for entities driven by the Bedrock animation pipeline.
 * Implementing this lets the entity renderer find the per-entity
 * {@link PetAnimator} without going through GeckoLib's {@code GeoEntity} chain.
 *
 * <p>Phase 2 only chiikawa implements this; the other six pets continue with
 * GeckoLib until Phase 4 moves the implementation up to {@code AbstractPet}.
 */
public interface ChiikawaAnimated {

    PetAnimator getPetAnimator();

    /**
     * Returns the short name of the animation that should loop on the main
     * channel right now (e.g. {@code "idle"}, {@code "run"}, {@code "sit"}).
     * The renderer prefixes this with the model key to find the
     * {@link AnimationLibrary} entry, falls back to {@code "idle"} if missing,
     * and calls {@link PetAnimator#setMain} (idempotent).
     *
     * @param walkSpeed normalized walk speed sampled from the entity, used to
     *                  distinguish stationary vs moving states
     */
    String getMainAnimationName(float walkSpeed);
}
