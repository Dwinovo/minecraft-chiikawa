package com.dwinovo.chiikawa.entity.brain.handler;

import java.util.List;
import com.dwinovo.chiikawa.entity.AbstractPet;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Brain wiring for the {@code NONE} pet job — pet is holding nothing
 * recognised. No job-specific activities to register; the universal
 * {@code CORE} (sit / look / pickup / ...) and {@code IDLE} (random walk)
 * activities are wired centrally in
 * {@link com.dwinovo.chiikawa.entity.AbstractPet#makeBrain}.
 */
public final class NoneJobHandler {
    private NoneJobHandler() {
    }

    /**
     * No-op: NoneJob owns no specialised activities. Kept as a symmetric
     * member of the JobHandler family so {@link com.dwinovo.chiikawa.entity.AbstractPet#makeBrain}
     * can call all jobs' {@code registerActivities} uniformly.
     */
    public static void registerActivities(Brain<AbstractPet> brain) {
    }

    public static void tickBrain(AbstractPet pet, Brain<AbstractPet> brain) {
        brain.setActiveActivityToFirstValid(List.of(Activity.IDLE));
    }
}
