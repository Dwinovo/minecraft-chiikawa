package com.dwinovo.chiikawa.entity.job.api;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * What a pet is able to do, assumed from the tool in its main hand (its job).
 *
 * <p>Brain wiring is static: every job's activities are registered once in
 * {@link AbstractPet#makeBrain}. A capability only names the intents it adds to the
 * generic ones; the intent selector decides which of them run. The numeric id is
 * what the synced {@code PET_JOB} value and the {@code PetJob} save key hold.
 */
public interface PetCapability {
    int id();

    /** Among capabilities the pet could assume, the highest priority wins. */
    int priority();

    boolean canAssume(AbstractPet pet);

    /** Ids of the intents this capability offers, see {@code PetIntents}. */
    List<ResourceLocation> intents();
}
