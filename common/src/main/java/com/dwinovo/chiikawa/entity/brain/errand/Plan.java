package com.dwinovo.chiikawa.entity.brain.errand;

import com.dwinovo.chiikawa.entity.brain.errand.view.PetView;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;

/**
 * What a pet has set out to do on an errand: this crop, that photo, a walk to the board. What the
 * errand's plans have in common is only what the rest after it asks of them.
 */
public interface Plan {
    /**
     * @param view the pet as it is now
     * @return where the plan takes the pet, worked out afresh each time since the owner, a friend or a
     *         partner moves; nothing for a plan that goes nowhere in particular: wandering, sitting,
     *         playing where it stands. It is all the area a pet acts in asks about an errand: an errand
     *         starts only on a destination within the anchor's reach and goes on only within its leash; one
     *         with no destination goes on only while the pet itself is within the leash
     */
    default Optional<GlobalPos> destination(PetView view) {
        return Optional.empty();
    }

    /**
     * @return what the plan is about, for a rest that is on that alone and not on the errand: the
     *         crop it could not reach, the partner it played with
     */
    default Optional<String> subject() {
        return Optional.empty();
    }

    /** @return the camera item or the scene the plan is for, for a rest whose length depends on it */
    default Optional<ResourceLocation> restKey() {
        return Optional.empty();
    }
}
