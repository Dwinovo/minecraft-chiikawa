package com.dwinovo.chiikawa.entity.job.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.job.api.PetCapability;

/**
 * Fallback pet job — pet is holding nothing recognised. Always assumable,
 * lowest priority, and has no work of its own: only the generic errands.
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
}
