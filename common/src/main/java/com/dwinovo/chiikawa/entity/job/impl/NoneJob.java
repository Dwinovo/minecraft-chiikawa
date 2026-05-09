package com.dwinovo.chiikawa.entity.job.impl;

import com.dwinovo.chiikawa.entity.AbstractPet;
import com.dwinovo.chiikawa.entity.brain.handler.NoneJobHandler;
import com.dwinovo.chiikawa.entity.job.api.IPetJob;
import net.minecraft.world.entity.ai.Brain;

/**
 * Fallback pet job — pet is holding nothing recognised. Always assumable,
 * lowest priority. Owns no specialised activities; just selects
 * {@link net.minecraft.world.entity.schedule.Activity#IDLE} each tick.
 */
public class NoneJob implements IPetJob {
    private final int id;

    public NoneJob(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean canAssume(AbstractPet pet) {
        return true;
    }

    @Override
    public void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        NoneJobHandler.tickBrain(pet, brain);
    }
}
