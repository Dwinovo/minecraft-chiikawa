package com.dwinovo.chiikawa.entity.job.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.job.api.PetCapability;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Fallback pet job — pet is holding nothing recognised. Always assumable,
 * lowest priority, and offers no intents beyond the generic ones.
 */
public class NoneJob implements PetCapability {
    private final int id;

    public NoneJob(int id) {
        this.id = id;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean canAssume(AbstractPet pet) {
        return true;
    }

    @Override
    public List<Identifier> intents() {
        return List.of();
    }
}
