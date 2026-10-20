package com.dwinovo.chiikawa.entity.brain.errand.view;

import com.dwinovo.chiikawa.social.PetInteraction;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * A scene a pet has thought of playing with another pet, as the pet's view of it: the pet that would be
 * visited is kept by its id, since it moves and a plan must not hold on to an entity. Only a scene the partner
 * would play along with now, and that the pet has what it takes for, is an idea the pet can act on.
 *
 * <p>Carries the scene itself rather than its id, so a data pack reloaded halfway through does not change
 * what the two are in the middle of.
 *
 * @param scene the scene's id
 * @param interaction the scene
 * @param self the part this pet plays
 * @param partnerRank which of the scene's partners the other pet is
 * @param partner the other pet
 * @param where where the other pet stands now
 */
public record SocialIdea(ResourceLocation scene, PetInteraction interaction, PetInteraction.Side self, int partnerRank,
        UUID partner, GlobalPos where) {
    /** @return the part the other pet plays */
    public PetInteraction.Side partnerSide() {
        return interaction.partners().get(partnerRank);
    }
}
