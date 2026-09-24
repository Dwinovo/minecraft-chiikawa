package com.dwinovo.chiikawa.entity.brain.constraint;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.PetTargeting;
import java.util.UUID;

/**
 * Who a pet belongs to. Derived from {@link net.minecraft.world.entity.TamableAnimal}'s
 * tame flag and owner, never stored separately.
 */
public sealed interface PetOwnership {
    Wild WILD = new Wild();

    record Wild() implements PetOwnership {
    }

    record Owned(UUID ownerId) implements PetOwnership {
    }

    static PetOwnership of(AbstractPet pet) {
        UUID ownerId = PetTargeting.ownerId(pet);
        return pet.isTame() && ownerId != null ? new Owned(ownerId) : WILD;
    }
}
