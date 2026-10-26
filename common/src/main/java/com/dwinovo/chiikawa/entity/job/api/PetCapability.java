package com.dwinovo.chiikawa.entity.job.api;

import com.dwinovo.chiikawa.entity.AbstractPet;
import java.util.Optional;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * What a pet is able to do, assumed from the tool in its main hand (its job).
 *
 * <p>Brain wiring is static: the brain is the same for every job, see
 * {@link AbstractPet#makeBrain}. The errands a capability adds to the generic ones are its
 * work, which data names, see {@link com.dwinovo.chiikawa.entity.job.JobWork}; the pet's mind
 * decides which of them run. The numeric id is what the synced {@code PET_JOB} value and the
 * {@code PetJob} save key hold.
 */
public interface PetCapability {
    int id();

    /** Among capabilities the pet could assume, the highest priority wins. */
    int priority();

    boolean canAssume(AbstractPet pet);

    /** @return the tag of the tools that make a pet assume this job, if the job is assumed by a tool */
    default Optional<TagKey<Item>> toolTag() {
        return Optional.empty();
    }
}
