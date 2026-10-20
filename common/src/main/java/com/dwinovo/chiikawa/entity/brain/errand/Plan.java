package com.dwinovo.chiikawa.entity.brain.errand;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * What a pet has set out to do on an errand: this crop, that photo, a walk to the board. What the
 * errand's plans have in common is only what the rest after it asks of them.
 */
public interface Plan {
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
