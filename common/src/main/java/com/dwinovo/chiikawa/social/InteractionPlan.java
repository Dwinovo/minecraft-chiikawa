package com.dwinovo.chiikawa.social;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@code INTERACTION_PLAN} memory: a scene a pet has thought of playing with another.
 * Written by the sensor with a short expiry, and only while the pet is on no scene; the pet
 * that acts on it lets it go when the scene is over. Not saved.
 *
 * <p>Carries the scene itself rather than its id, so a data pack reloaded halfway through
 * does not change what the two are in the middle of.
 *
 * @param id the scene's id
 * @param interaction the scene
 * @param self the part this pet plays
 * @param partner the other pet
 * @param partnerSide the part the other pet plays
 */
public record InteractionPlan(ResourceLocation id, PetInteraction interaction, PetInteraction.Side self,
        AbstractPet partner, PetInteraction.Side partnerSide) {
}
