package com.dwinovo.chiikawa.social;

import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.resources.Identifier;

/**
 * The {@code INTERACTION_PLAN} memory: a scene a pet has thought of playing with another.
 * Written by the sensor with a short expiry while it is only an idea; once the pet sets off
 * it is engaged, and the sensor leaves it alone until the scene is over. Not saved.
 *
 * <p>Carries the scene itself rather than its id, so a data pack reloaded halfway through
 * does not change what the two are in the middle of.
 *
 * @param id the scene's id
 * @param interaction the scene
 * @param self the part this pet plays
 * @param partner the other pet
 * @param partnerSide the part the other pet plays
 * @param engaged whether the pet has set off
 */
public record InteractionPlan(Identifier id, PetInteraction interaction, PetInteraction.Side self,
        AbstractPet partner, PetInteraction.Side partnerSide, boolean engaged) {
    /** @return this plan, now under way */
    public InteractionPlan engage() {
        return new InteractionPlan(id, interaction, self, partner, partnerSide, true);
    }
}
