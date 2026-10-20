package com.dwinovo.chiikawa.entity.brain.errand.run;

import com.dwinovo.chiikawa.anim.state.PetActivity;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Where the executor publishes what it derives for the player's game: which errand the pet is on, the
 * pose it holds, the scene it performs. The pet implements it by setting its synced values, so
 * the executor never touches the pet, and nothing else sets what this sets.
 */
public interface Projection {
    /** @param errand the errand the pet is on, or {@code null} for none */
    void intent(@Nullable ResourceLocation errand);

    void activity(PetActivity activity);

    /** @param performance the scene animation, or empty for none */
    void performance(String performance);
}
